package com.project.paymentservice.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.domain.model.OutboxEntity;
import com.project.paymentservice.domain.repository.OutboxRepository;
import com.project.paymentservice.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxRecordListener {

    private static final String AGGREGATE_TYPE_PAYMENT = "PAYMENT";
    private static final String EVENT_TYPE_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String EVENT_TYPE_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordPaymentCompletedOutbox(PaymentCompletedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEntity outbox = OutboxEntity.create(
                AGGREGATE_TYPE_PAYMENT,
                event.getAggregateId(),
                EVENT_TYPE_PAYMENT_COMPLETED,
                payload,
                KafkaConfig.TOPIC_PAYMENT_COMPLETED,
                event.getMessageKey()
            );

            outboxRepository.save(outbox);
            log.debug("[Outbox Record] 결제 완료 이벤트 기록 - salesId: {}, orderId: {}",
                event.getSalesId(), event.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("[Outbox Record] 이벤트 직렬화 실패 - salesId: {}", event.getSalesId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordPaymentRefundedOutbox(PaymentRefundedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEntity outbox = OutboxEntity.create(
                AGGREGATE_TYPE_PAYMENT,
                event.getAggregateId(),
                EVENT_TYPE_PAYMENT_REFUNDED,
                payload,
                KafkaConfig.TOPIC_PAYMENT_REFUNDED,
                event.getMessageKey()
            );

            outboxRepository.save(outbox);
            log.debug("[Outbox Record] 환불 이벤트 기록 - salesId: {}", event.getSalesId());

        } catch (JsonProcessingException e) {
            log.error("[Outbox Record] 이벤트 직렬화 실패 - salesId: {}", event.getSalesId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
