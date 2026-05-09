package com.project.productservice.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.productservice.domain.model.OutboxEntity;
import com.project.productservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.productservice.domain.repository.OutboxRepository;
import com.project.productservice.infrastructure.kafka.ProductCreatedEvent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 메시지 릴레이 (29CM 방식)
 *
 * SEND_SUCCESS가 아니면서 created_at이 현 시간 기준으로 10분 이상 지난 것들을
 * 주기적으로 확인하면서 이벤트 재발행을 시도
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxMessageRelay {

    private static final String TOPIC_PRODUCT_CREATED = "product-created";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${outbox.relay.threshold-minutes:10}")
    private int thresholdMinutes;

    @Value("${outbox.relay.batch-size:100}")
    private int batchSize;

    @Value("${outbox.relay.max-retries:3}")
    private int maxRetries;

    @Value("${outbox.relay.cleanup-days:7}")
    private int cleanupDays;

    /**
     * 재발행 대상 메시지 처리
     * - SEND_SUCCESS가 아니면서 created_at이 현 시간 기준으로 threshold 이상 지난 것들
     */
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

    /**
     * 처리 완료된 오래된 메시지 정리
     */
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
            ProductCreatedEvent event = objectMapper.readValue(
                outbox.getPayload(), ProductCreatedEvent.class);

            String key = String.valueOf(event.getProductId());

            // 동기적으로 Kafka 발행
            kafkaTemplate.send(TOPIC_PRODUCT_CREATED, key, event).get();

            outbox.markAsSendSuccess();
            log.info("[Outbox Relay] 재발행 성공 - productId: {}, outboxId: {}, retryCount: {}",
                event.getProductId(), outbox.getId(), outbox.getRetryCount());

        } catch (Exception e) {
            outbox.markAsSendFail(e.getMessage());
            log.error("[Outbox Relay] 재발행 실패 - outboxId: {}, retryCount: {}, error: {}",
                outbox.getId(), outbox.getRetryCount(), e.getMessage());
        }
    }
}
