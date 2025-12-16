package com.project.paymentservice.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.domain.model.OutboxEntity;
import com.project.paymentservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.paymentservice.domain.repository.OutboxRepository;
import com.project.paymentservice.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AFTER_COMMIT 리스너
 * 트랜잭션 커밋 후 즉시 Kafka로 발행 (낮은 지연시간)
 * 실패해도 Outbox Relay가 재발행 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublishListener {

    private static final String EVENT_TYPE_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String EVENT_TYPE_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishPaymentCompleted(PaymentCompletedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.getMessageKey(), payload)
                .get(5, TimeUnit.SECONDS);

            // Outbox 상태 업데이트
            markOutboxSuccess(event.getAggregateId(), EVENT_TYPE_PAYMENT_COMPLETED);

            log.info("[Kafka] 결제 완료 이벤트 발행 - orderId: {}, salesId: {}",
                event.getOrderId(), event.getSalesId());

        } catch (Exception e) {
            log.warn("[Kafka] 결제 완료 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                event.getSalesId(), e.getMessage());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishPaymentRefunded(PaymentRefundedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_REFUNDED, event.getMessageKey(), payload)
                .get(5, TimeUnit.SECONDS);

            // Outbox 상태 업데이트
            markOutboxSuccess(event.getAggregateId(), EVENT_TYPE_PAYMENT_REFUNDED);

            log.info("[Kafka] 환불 이벤트 발행 - salesId: {}", event.getSalesId());

        } catch (Exception e) {
            log.warn("[Kafka] 환불 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                event.getSalesId(), e.getMessage());
        }
    }

    private void markOutboxSuccess(String aggregateId, String eventType) {
        try {
            List<OutboxEntity> outboxList = outboxRepository
                .findByAggregateIdAndEventTypeAndStatus(aggregateId, eventType, OutboxStatus.INIT);

            for (OutboxEntity outbox : outboxList) {
                outbox.markAsSendSuccess();
            }
        } catch (Exception e) {
            log.warn("[Outbox] 상태 업데이트 실패 - aggregateId: {}, eventType: {}", aggregateId, eventType);
        }
    }
}
