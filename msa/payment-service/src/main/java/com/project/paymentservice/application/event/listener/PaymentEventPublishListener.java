package com.project.paymentservice.application.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.paymentservice.domain.repository.OutboxRepository;
import com.project.paymentservice.infrastructure.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

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
        var previousMdc = MDC.getCopyOfContextMap();
        try {
            MDC.put("orderId", event.getOrderId());
            MDC.put("salesId", String.valueOf(event.getSalesId()));
            String payload = objectMapper.writeValueAsString(event);

            CompletableFuture<?> sendFuture = kafkaTemplate.send(
                KafkaConfig.TOPIC_PAYMENT_COMPLETED, event.getMessageKey(), payload);
            sendFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    // Outbox 상태 업데이트
                    markOutboxSuccess(event.getAggregateId(), EVENT_TYPE_PAYMENT_COMPLETED);
                    log.info("[Kafka] 결제 완료 이벤트 발행 - orderId: {}, salesId: {}",
                        event.getOrderId(), event.getSalesId());
                } else {
                    markOutboxFail(event.getAggregateId(), EVENT_TYPE_PAYMENT_COMPLETED, ex.getMessage());
                    log.warn("[Kafka] 결제 완료 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                        event.getSalesId(), ex.getMessage());
                }
            });

        } catch (Exception e) {
            markOutboxFail(event.getAggregateId(), EVENT_TYPE_PAYMENT_COMPLETED, e.getMessage());
            log.warn("[Kafka] 결제 완료 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                event.getSalesId(), e.getMessage());
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishPaymentRefunded(PaymentRefundedInternalEvent event) {
        var previousMdc = MDC.getCopyOfContextMap();
        try {
            MDC.put("orderId", event.getOrderId());
            MDC.put("salesId", String.valueOf(event.getSalesId()));
            String payload = objectMapper.writeValueAsString(event);

            CompletableFuture<?> sendFuture = kafkaTemplate.send(
                KafkaConfig.TOPIC_PAYMENT_REFUNDED, event.getMessageKey(), payload);
            sendFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    // Outbox 상태 업데이트
                    markOutboxSuccess(event.getAggregateId(), EVENT_TYPE_PAYMENT_REFUNDED);
                    log.info("[Kafka] 환불 이벤트 발행 - salesId: {}", event.getSalesId());
                } else {
                    markOutboxFail(event.getAggregateId(), EVENT_TYPE_PAYMENT_REFUNDED, ex.getMessage());
                    log.warn("[Kafka] 환불 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                        event.getSalesId(), ex.getMessage());
                }
            });

        } catch (Exception e) {
            markOutboxFail(event.getAggregateId(), EVENT_TYPE_PAYMENT_REFUNDED, e.getMessage());
            log.warn("[Kafka] 환불 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                event.getSalesId(), e.getMessage());
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    }

    private void markOutboxSuccess(String aggregateId, String eventType) {
        try {
            outboxRepository.updateStatusSuccessByAggregateIdAndEventType(
                aggregateId,
                eventType,
                OutboxStatus.INIT,
                OutboxStatus.SEND_SUCCESS,
                LocalDateTime.now()
            );
        } catch (Exception e) {
            log.warn("[Outbox] 상태 업데이트 실패 - aggregateId: {}, eventType: {}", aggregateId, eventType);
        }
    }

    private void markOutboxFail(String aggregateId, String eventType, String errorMessage) {
        try {
            outboxRepository.updateStatusFailByAggregateIdAndEventType(
                aggregateId,
                eventType,
                OutboxStatus.INIT,
                OutboxStatus.SEND_FAIL,
                truncateErrorMessage(errorMessage)
            );
        } catch (Exception e) {
            log.warn("[Outbox] 실패 상태 업데이트 실패 - aggregateId: {}, eventType: {}", aggregateId, eventType);
        }
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
