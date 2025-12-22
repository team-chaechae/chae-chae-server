package com.project.common.outbox.infrastructure;

import com.project.common.outbox.domain.OutboxEntity;
import com.project.common.outbox.domain.OutboxRepository;
import com.project.common.outbox.domain.OutboxStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractOutboxMessageRelay<T extends OutboxEntity> {

    protected abstract OutboxRepository<T> getOutboxRepository();
    protected abstract KafkaTemplate<String, String> getKafkaTemplate();
    protected abstract int getThresholdMinutes();
    protected abstract int getBatchSize();
    protected abstract int getMaxRetries();
    protected abstract int getCleanupDays();

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms:60000}")
    @Transactional
    public void relayFailedMessages() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(getThresholdMinutes());

        List<T> messages = getOutboxRepository().findMessagesForRetry(
                OutboxStatus.SEND_SUCCESS,
                threshold,
                getMaxRetries(),
                getBatchSize()
        );

        if (messages.isEmpty()) {
            return;
        }

        log.info("[Outbox Relay] 재발행 대상 메시지 {}건 발견", messages.size());

        for (T outbox : messages) {
            retryPublish(outbox);
        }
    }

    @Scheduled(cron = "${outbox.relay.cleanup-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupProcessedMessages() {
        LocalDateTime before = LocalDateTime.now().minusDays(getCleanupDays());
        int deleted = getOutboxRepository().deleteProcessedMessagesBefore(OutboxStatus.SEND_SUCCESS, before);

        if (deleted > 0) {
            log.info("[Outbox Cleanup] {}일 이전 처리 완료 메시지 {}건 삭제", getCleanupDays(), deleted);
        }
    }

    protected void retryPublish(T outbox) {
        try {
            getKafkaTemplate().send(outbox.getTopic(), outbox.getMessageKey(), outbox.getPayload())
                    .get(5, TimeUnit.SECONDS);

            outbox.markAsSendSuccess();
            log.info("[Outbox Relay] 재발행 성공 - topic: {}, key: {}, outboxId: {}, retryCount: {}",
                    outbox.getTopic(), outbox.getMessageKey(), outbox.getId(), outbox.getRetryCount());

        } catch (Exception e) {
            outbox.markAsSendFail(e.getMessage());
            log.error("[Outbox Relay] 재발행 실패 - outboxId: {}, retryCount: {}, error: {}",
                    outbox.getId(), outbox.getRetryCount(), e.getMessage());
        }
    }
}
