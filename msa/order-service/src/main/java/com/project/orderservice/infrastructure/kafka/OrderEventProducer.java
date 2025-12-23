package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.repository.OutboxRepository;
import com.project.orderservice.infrastructure.kafka.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 이벤트 Kafka Producer (Outbox 패턴)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxRepository outboxRepository;

    /**
     * 주문 생성 이벤트 발행 + Outbox 상태 업데이트
     */
    public void publishOrderCreated(OrderCreatedEvent event, String aggregateId) {
        String key = event.getOrderId();

        // 비동기 발행 (Outbox Relay가 실패 시 재시도)
        kafkaTemplate.send(TOPIC_ORDER_CREATED, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        markOutboxSuccess(aggregateId);
                        log.info("[Kafka] 주문 생성 이벤트 발행 완료 - orderId: {}, salesId: {}",
                                event.getOrderId(), event.getSalesId());
                    } else {
                        log.warn("[Kafka] 주문 생성 이벤트 발행 실패 (Outbox Relay가 재시도) - orderId: {}, error: {}",
                                event.getOrderId(), ex.getMessage());
                    }
                });
    }

    @Transactional
    public void markOutboxSuccess(String aggregateId) {
        try {
            List<OutboxEntity> outboxList = outboxRepository
                    .findByAggregateIdAndEventTypeAndStatus(aggregateId, EVENT_TYPE_ORDER_CREATED, OutboxStatus.INIT);

            for (OutboxEntity outbox : outboxList) {
                outbox.markAsSendSuccess();
            }
        } catch (Exception e) {
            log.warn("[Outbox] 상태 업데이트 실패 - aggregateId: {}", aggregateId);
        }
    }
}
