package com.project.paymentservice.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.infrastructure.config.KafkaConfig;
import com.project.paymentservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import com.project.paymentservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 결제 완료 이벤트 발행 (Order, Inventory 서비스에서 소비)
     */
    public void publishPaymentCompleted(String orderId, Long salesId, Integer totalAmount) {
        try {
            PaymentCompletedEvent event = PaymentCompletedEvent.of(orderId, salesId, totalAmount);
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_COMPLETED, orderId, message);
            log.info("[Kafka] 결제 완료 이벤트 발행 - orderId: {}, salesId: {}", orderId, salesId);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 메시지 직렬화 실패 - orderId: {}, salesId: {}", orderId, salesId, e);
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }

    public void publishPaymentRefunded(Long salesId) {
        try {
            PaymentRefundedEvent event = PaymentRefundedEvent.of(salesId);
            String message = objectMapper.writeValueAsString(event);
            String key = String.valueOf(salesId);

            kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENT_REFUNDED, key, message);
            log.info("[Kafka] 환불 이벤트 발행 - salesId: {}", salesId);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] 메시지 직렬화 실패 - salesId: {}", salesId, e);
            throw new RuntimeException("메시지 직렬화 실패", e);
        }
    }
}
