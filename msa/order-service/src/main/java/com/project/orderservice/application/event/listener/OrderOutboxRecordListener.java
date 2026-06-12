package com.project.orderservice.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.application.event.DeliveryCancelRequestedInternalEvent;
import com.project.orderservice.application.event.DeliveryCreateRequestedInternalEvent;
import com.project.orderservice.application.event.OrderCreatedInternalEvent;
import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * BEFORE_COMMIT 리스너
 * 트랜잭션 커밋 전에 Outbox 테이블에 이벤트를 기록
 * 도메인 로직과 같은 트랜잭션으로 묶여서 원자성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxRecordListener {

    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final String EVENT_TYPE_DELIVERY_CREATE_REQUESTED = "DELIVERY_CREATE_REQUESTED";
    private static final String EVENT_TYPE_DELIVERY_CANCEL_REQUESTED = "DELIVERY_CANCEL_REQUESTED";
    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String TOPIC_DELIVERY_CREATE_REQUESTED = "delivery-create-requested";
    private static final String TOPIC_DELIVERY_CANCEL_REQUESTED = "delivery-cancel-requested";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordOrderCreatedOutbox(OrderCreatedInternalEvent event) {
        saveOutbox(
                event,
                event.getAggregateId(),
                EVENT_TYPE_ORDER_CREATED,
                TOPIC_ORDER_CREATED,
                event.getMessageKey(),
                event.getSalesId(),
                "주문 생성"
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordDeliveryCreateRequestedOutbox(DeliveryCreateRequestedInternalEvent event) {
        saveOutbox(
                event,
                event.getAggregateId(),
                EVENT_TYPE_DELIVERY_CREATE_REQUESTED,
                TOPIC_DELIVERY_CREATE_REQUESTED,
                event.getMessageKey(),
                event.getSalesId(),
                "배송 생성 요청"
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordDeliveryCancelRequestedOutbox(DeliveryCancelRequestedInternalEvent event) {
        saveOutbox(
                event,
                event.getAggregateId(),
                EVENT_TYPE_DELIVERY_CANCEL_REQUESTED,
                TOPIC_DELIVERY_CANCEL_REQUESTED,
                event.getMessageKey(),
                event.getSalesId(),
                "배송 취소 요청"
        );
    }

    private void saveOutbox(
            Object event,
            String aggregateId,
            String eventType,
            String topic,
            String messageKey,
            Long salesId,
            String eventName
    ) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEntity outbox = OutboxEntity.create(
                AGGREGATE_TYPE_ORDER,
                aggregateId,
                eventType,
                payload,
                topic,
                messageKey
            );

            outboxRepository.save(outbox);
            log.debug("[Outbox Record] {} 이벤트 기록 - salesId: {}, topic: {}",
                    eventName, salesId, topic);

        } catch (JsonProcessingException e) {
            log.error("[Outbox Record] 이벤트 직렬화 실패 - salesId: {}, eventType: {}",
                    salesId, eventType, e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
