package com.project.inventoryservice.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.inventoryservice.domain.model.OutboxEntity;
import com.project.inventoryservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.inventoryservice.domain.repository.OutboxRepository;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 재고 차감 성공 이벤트 Producer (Outbox 패턴)
 * inventory-confirmed 토픽으로 발행하여 order-service에서 SSE 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConfirmedEventProducer {

    private static final String TOPIC = "inventory-confirmed";
    private static final String EVENT_TYPE = "INVENTORY_CONFIRMED";

    private final KafkaTemplate<String, Object> objectKafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Outbox에 저장 (트랜잭션 내에서 호출)
     */
    @Transactional
    public void saveToOutbox(InventoryConfirmedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntity outbox = OutboxEntity.create(
                    "INVENTORY",
                    String.valueOf(event.getSalesId()),
                    EVENT_TYPE,
                    payload,
                    TOPIC,
                    event.getOrderId()
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    /**
     * 즉시 발행 + Outbox 상태 업데이트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(InventoryConfirmedEvent event) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        String aggregateId = String.valueOf(salesId);

        // 먼저 Outbox에 저장
        saveToOutbox(event);

        try {
            objectKafkaTemplate.send(TOPIC, orderId, event)
                    .get(5, TimeUnit.SECONDS);

            // Outbox 상태 업데이트
            markOutboxSuccess(aggregateId);

            log.info("[Kafka] 재고 차감 성공 이벤트 발행 - orderId: {}, salesId: {}",
                    orderId, salesId);

        } catch (Exception e) {
            log.warn("[Kafka] 재고 차감 성공 이벤트 발행 실패 (Outbox Relay가 재시도) - orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getMessage());
        }
    }

    private void markOutboxSuccess(String aggregateId) {
        try {
            List<OutboxEntity> outboxList = outboxRepository
                    .findByAggregateIdAndEventTypeAndStatus(aggregateId, EVENT_TYPE, OutboxStatus.INIT);

            for (OutboxEntity outbox : outboxList) {
                outbox.markAsSendSuccess();
            }
        } catch (Exception e) {
            log.warn("[Outbox] 상태 업데이트 실패 - aggregateId: {}", aggregateId);
        }
    }
}
