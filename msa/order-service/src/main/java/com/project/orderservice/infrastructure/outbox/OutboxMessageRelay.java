package com.project.orderservice.infrastructure.outbox;

import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    @Value("${outbox.relay.threshold-minutes:10}")
    private int thresholdMinutes;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:3}")
    private int maxRetries;

    @Value("${outbox.relay.max-age-seconds:60}")
    private long maxAgeSeconds;

    @Value("${outbox.relay.base-backoff-ms:1000}")
    private long baseBackoffMs;

    @Value("${outbox.relay.max-backoff-ms:60000}")
    private long maxBackoffMs;

    @Value("${outbox.relay.send-timeout-seconds:10}")
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
            Arrays.asList(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
            threshold,
            maxRetries,
            PageRequest.of(0, batchSize)
        );

        if (messages.isEmpty()) {
            return;
        }

        log.info("[Outbox Relay] 재발행 대상 메시지 {}건 발견", messages.size());

        for (OutboxEntity outbox : messages) {
            if (!isBackoffElapsed(outbox)) {
                continue;
            }
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
            CompletableFuture<?> sendFuture = stringKafkaTemplate.send(
                outbox.getTopic(), outbox.getMessageKey(), outbox.getPayload());
            sendFuture.get(sendTimeoutSeconds, TimeUnit.SECONDS);
            outboxRepository.updateStatusSuccessById(
                outbox.getId(),
                OutboxStatus.SEND_SUCCESS,
                LocalDateTime.now()
            );
            log.info("[Outbox Relay] 재발행 성공 - topic: {}, key: {}, outboxId: {}, retryCount: {}",
                outbox.getTopic(), outbox.getMessageKey(), outbox.getId(), outbox.getRetryCount());

        } catch (Exception e) {
            String errorMessage = resolveErrorMessage(e);
            handlePublishFailure(outbox, errorMessage);
            log.error("[Outbox Relay] 재발행 실패 - outboxId: {}, retryCount: {}, error: {}",
                outbox.getId(), outbox.getRetryCount(), errorMessage);
        }
    }

    private void handlePublishFailure(OutboxEntity outbox, String errorMessage) {
        int nextRetryCount = outbox.getRetryCount() + 1;
        if (nextRetryCount >= maxRetries || isExpired(outbox)) {
            if (publishToDlq(outbox, errorMessage)) {
                outboxRepository.updateStatusFailByIdWithRetryCount(
                    outbox.getId(),
                    OutboxStatus.SEND_FAIL,
                    maxRetries,
                    truncateErrorMessage(errorMessage),
                    LocalDateTime.now()
                );
                return;
            }

            outboxRepository.updateStatusFailByIdWithRetryCount(
                outbox.getId(),
                OutboxStatus.SEND_FAIL,
                Math.max(0, maxRetries - 1),
                truncateErrorMessage("DLQ publish failed: " + errorMessage),
                LocalDateTime.now()
            );
            return;
        }

        outboxRepository.updateStatusFailById(
            outbox.getId(),
            OutboxStatus.SEND_FAIL,
            truncateErrorMessage(errorMessage),
            LocalDateTime.now()
        );
    }

    private boolean publishToDlq(OutboxEntity outbox, String errorMessage) {
        String dlqTopic = outbox.getTopic() + dlqTopicSuffix;
        try {
            stringKafkaTemplate.send(dlqTopic, outbox.getMessageKey(), outbox.getPayload())
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

    private boolean isBackoffElapsed(OutboxEntity outbox) {
        LocalDateTime lastAttempt = outbox.getProcessedAt() != null
            ? outbox.getProcessedAt()
            : outbox.getCreatedAt();
        long backoffMs = calculateBackoffMs(outbox.getRetryCount());
        return lastAttempt.plusNanos(backoffMs * 1_000_000).isBefore(LocalDateTime.now());
    }

    private boolean isExpired(OutboxEntity outbox) {
        if (outbox.getCreatedAt() == null) {
            return false;
        }
        return outbox.getCreatedAt()
            .plusNanos(maxAgeSeconds * 1_000_000_000L)
            .isBefore(LocalDateTime.now());
    }

    private long calculateBackoffMs(int retryCount) {
        if (retryCount <= 0) {
            return 0;
        }
        int exponent = Math.min(30, retryCount - 1);
        long backoff = baseBackoffMs * (1L << exponent);
        return Math.min(backoff, maxBackoffMs);
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
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
