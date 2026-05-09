package com.project.paymentservice.infrastructure.outbox;

import com.project.paymentservice.domain.model.OutboxEntity;
import com.project.paymentservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.paymentservice.domain.repository.OutboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${outbox.relay.threshold-minutes:10}")
    private int thresholdMinutes;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:3}")
    private int maxRetries;

    @Value("${outbox.relay.cleanup-days:7}")
    private int cleanupDays;

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:60000}")
    @Transactional
    public void relayFailedMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(thresholdMinutes);

        List<OutboxEntity> messages = outboxRepository.findMessagesForRetry(
            OutboxStatus.SEND_SUCCESS,
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
            CompletableFuture<?> sendFuture = kafkaTemplate.send(
                outbox.getTopic(), outbox.getMessageKey(), outbox.getPayload());
            sendFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    outboxRepository.updateStatusSuccessById(
                        outbox.getId(),
                        OutboxStatus.SEND_SUCCESS,
                        LocalDateTime.now()
                    );
                    log.info("[Outbox Relay] 재발행 성공 - topic: {}, key: {}, outboxId: {}, retryCount: {}",
                        outbox.getTopic(), outbox.getMessageKey(), outbox.getId(), outbox.getRetryCount());
                } else {
                    outboxRepository.updateStatusFailById(
                        outbox.getId(),
                        OutboxStatus.SEND_FAIL,
                        truncateErrorMessage(ex.getMessage())
                    );
                    log.error("[Outbox Relay] 재발행 실패 - outboxId: {}, retryCount: {}, error: {}",
                        outbox.getId(), outbox.getRetryCount(), ex.getMessage());
                }
            });

        } catch (Exception e) {
            outboxRepository.updateStatusFailById(
                outbox.getId(),
                OutboxStatus.SEND_FAIL,
                truncateErrorMessage(e.getMessage())
            );
            log.error("[Outbox Relay] 재발행 실패 - outboxId: {}, retryCount: {}, error: {}",
                outbox.getId(), outbox.getRetryCount(), e.getMessage());
        }
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
