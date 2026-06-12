package com.project.deliveryservice.infrastructure.outbox;

import com.project.deliveryservice.domain.model.OutboxEntity;
import com.project.deliveryservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.deliveryservice.domain.repository.OutboxRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    @Value("${outbox.relay.threshold-minutes:10}")
    private int thresholdMinutes;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:3}")
    private int maxRetries;

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
                threshold,
                maxRetries,
                batchSize
        );

        for (OutboxEntity outbox : messages) {
            retryPublish(outbox);
        }
    }

    @Scheduled(cron = "${outbox.relay.cleanup-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupProcessedMessages() {
        LocalDateTime before = LocalDateTime.now().minusDays(cleanupDays);
        outboxRepository.deleteProcessedMessagesBefore(OutboxStatus.SEND_SUCCESS, before);
    }

    private void retryPublish(OutboxEntity outbox) {
        try {
            stringKafkaTemplate.send(outbox.getTopic(), outbox.getMessageKey(), outbox.getPayload())
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            outboxRepository.updateStatusSuccessById(outbox.getId(), OutboxStatus.SEND_SUCCESS, LocalDateTime.now());
            log.info("delivery_outbox_relay_success outboxId={} topic={} key={}",
                    outbox.getId(), outbox.getTopic(), outbox.getMessageKey());
        } catch (Exception e) {
            handlePublishFailure(outbox, resolveErrorMessage(e));
        }
    }

    private void handlePublishFailure(OutboxEntity outbox, String errorMessage) {
        int nextRetryCount = outbox.getRetryCount() + 1;
        if (nextRetryCount >= maxRetries && publishToDlq(outbox, errorMessage)) {
            outboxRepository.updateStatusFailByIdWithRetryCount(
                    outbox.getId(),
                    OutboxStatus.DLQ_SENT,
                    maxRetries,
                    truncate(errorMessage),
                    LocalDateTime.now()
            );
            return;
        }

        outboxRepository.updateStatusFailByIdWithRetryCount(
                outbox.getId(),
                OutboxStatus.SEND_FAIL,
                nextRetryCount,
                truncate(errorMessage),
                LocalDateTime.now()
        );
    }

    private boolean publishToDlq(OutboxEntity outbox, String errorMessage) {
        try {
            stringKafkaTemplate.send(outbox.getTopic() + dlqTopicSuffix, outbox.getMessageKey(), outbox.getPayload())
                    .get(sendTimeoutSeconds, TimeUnit.SECONDS);
            log.error("delivery_outbox_relay_dlq_sent outboxId={} topic={} error={}",
                    outbox.getId(), outbox.getTopic() + dlqTopicSuffix, errorMessage);
            return true;
        } catch (Exception e) {
            log.error("delivery_outbox_relay_dlq_failed outboxId={} error={}",
                    outbox.getId(), resolveErrorMessage(e));
            return false;
        }
    }

    private String resolveErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
