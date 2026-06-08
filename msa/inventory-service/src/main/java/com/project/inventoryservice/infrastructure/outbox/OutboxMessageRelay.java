package com.project.inventoryservice.infrastructure.outbox;

import com.project.inventoryservice.domain.model.OutboxEntity;
import com.project.inventoryservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.inventoryservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> objectKafkaTemplate;

    @Value("${outbox.relay.threshold-minutes:10}")
    private int thresholdMinutes;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:3}")
    private int maxRetries;

    @Value("${outbox.relay.send-timeout-seconds:5}")
    private long sendTimeoutSeconds;

    @Value("${dlq.topic-suffix:.dlq}")
    private String dlqTopicSuffix;

    @Value("${outbox.relay.cleanup-days:7}")
    private int cleanupDays;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:60000}")
    @Transactional
    public void relayFailedMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);

        List<OutboxEntity> messages = outboxRepository.findMessagesForRetry(
            OutboxStatus.SEND_SUCCESS,
            Arrays.asList(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
            threshold,
            maxRetries,
            batchSize
        );

        if (messages.isEmpty()) {
            return;
        }

        log.info("[Outbox Relay] 재발행 대상 메시지 {}건 발견", messages.size());

        for (OutboxEntity outbox : messages) {
            retryPublish(outbox);
        }
    }

    @Scheduled(cron = "${outbox.relay.cleanup-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupProcessedMessages() {
        LocalDateTime before = LocalDateTime.now().minusDays(cleanupDays);
        int deleted = outboxRepository.deleteProcessedMessagesBefore(OutboxStatus.SEND_SUCCESS, before);

        if (deleted > 0) {
            log.info("[Outbox Cleanup] {}일 이전 처리 완료 메시지 {}건 삭제", cleanupDays, deleted);
        }
    }

    private void retryPublish(OutboxEntity outbox) {
        try {
            objectKafkaTemplate.send(outbox.getTopic(), outbox.getMessageKey(), outbox.getPayload())
                .get(sendTimeoutSeconds, TimeUnit.SECONDS);

            outbox.markAsSendSuccess();
            log.info("[Outbox Relay] 재발행 성공 - topic: {}, key: {}, outboxId: {}, retryCount: {}",
                outbox.getTopic(), outbox.getMessageKey(), outbox.getId(), outbox.getRetryCount());

        } catch (Exception e) {
            handlePublishFailure(outbox, resolveErrorMessage(e));
            log.error("[Outbox Relay] 재발행 실패 - outboxId: {}, retryCount: {}, error: {}",
                outbox.getId(), outbox.getRetryCount(), resolveErrorMessage(e));
        }
    }

    private void handlePublishFailure(OutboxEntity outbox, String errorMessage) {
        int nextRetryCount = outbox.getRetryCount() + 1;
        if (nextRetryCount >= maxRetries) {
            if (publishToDlq(outbox, errorMessage)) {
                outbox.markAsDlqSent(errorMessage);
                return;
            }
            outbox.markAsDlqFail("DLQ publish failed: " + errorMessage, Math.max(0, maxRetries - 1));
            return;
        }

        outbox.markAsSendFail(errorMessage);
    }

    private boolean publishToDlq(OutboxEntity outbox, String errorMessage) {
        String dlqTopic = outbox.getTopic() + dlqTopicSuffix;
        try {
            objectKafkaTemplate.send(dlqTopic, outbox.getMessageKey(), outbox.getPayload())
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            log.error("[Outbox Relay] DLQ 전송 - topic: {}, outboxId: {}, error: {}",
                    dlqTopic, outbox.getId(), errorMessage);
            return true;
        } catch (Exception e) {
            log.error("[Outbox Relay] DLQ 전송 실패 - topic: {}, outboxId: {}, error: {}",
                    dlqTopic, outbox.getId(), resolveErrorMessage(e));
            return false;
        }
    }

    private String resolveErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }
}
