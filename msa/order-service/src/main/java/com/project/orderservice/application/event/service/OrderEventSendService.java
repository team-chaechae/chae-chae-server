package com.project.orderservice.application.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.application.event.OrderCreatedInternalEvent;
import com.project.orderservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.orderservice.domain.repository.OutboxRepository;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderEventSendService {

    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED";
    private static final int SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderEventSendService(
            @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void sendOrderCreated(OrderCreatedInternalEvent event) {
        var previousMdc = MDC.getCopyOfContextMap();
        try {
            MDC.put("orderId", event.getOrderId());
            MDC.put("salesId", String.valueOf(event.getSalesId()));

            try {
                String payload = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(TOPIC_ORDER_CREATED, event.getMessageKey(), payload)
                        .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                String errorMessage = resolveErrorMessage(e);
                markOutboxFail(event.getAggregateId(), EVENT_TYPE_ORDER_CREATED, errorMessage);
                log.warn("[Kafka] 주문 생성 이벤트 발행 실패 (Outbox Relay가 재시도) - salesId: {}, error: {}",
                        event.getSalesId(), errorMessage);
                return;
            }

            markOutboxSuccess(event.getAggregateId(), EVENT_TYPE_ORDER_CREATED);
            log.info("[Kafka] 주문 생성 이벤트 발행 - orderId: {}, salesId: {}",
                    event.getOrderId(), event.getSalesId());
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    }

    private void markOutboxSuccess(String aggregateId, String eventType) {
        int updated = outboxRepository.updateStatusSuccessByAggregateIdAndEventType(
                aggregateId,
                eventType,
                OutboxStatus.INIT,
                OutboxStatus.SEND_SUCCESS,
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new IllegalStateException(
                    "Outbox 성공 상태 업데이트 실패 - aggregateId: " + aggregateId + ", eventType: " + eventType
            );
        }
    }

    private void markOutboxFail(String aggregateId, String eventType, String errorMessage) {
        int updated = outboxRepository.updateStatusFailByAggregateIdAndEventType(
                aggregateId,
                eventType,
                OutboxStatus.INIT,
                OutboxStatus.SEND_FAIL,
                truncateErrorMessage(errorMessage),
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new IllegalStateException(
                    "Outbox 실패 상태 업데이트 실패 - aggregateId: " + aggregateId + ", eventType: " + eventType
            );
        }
    }

    private String truncateErrorMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String resolveErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getMessage();
        }
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }
}
