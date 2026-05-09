package com.project.orderservice.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String TOPIC_ORDER_CREATED = "order-created";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordOrderCreatedOutbox(OrderCreatedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEntity outbox = OutboxEntity.create(
                AGGREGATE_TYPE_ORDER,
                event.getAggregateId(),
                EVENT_TYPE_ORDER_CREATED,
                payload,
                TOPIC_ORDER_CREATED,
                event.getMessageKey()
            );

            outboxRepository.save(outbox);
            log.debug("[Outbox Record] 주문 생성 이벤트 기록 - salesId: {}, orderId: {}",
                event.getSalesId(), event.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("[Outbox Record] 이벤트 직렬화 실패 - salesId: {}", event.getSalesId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
