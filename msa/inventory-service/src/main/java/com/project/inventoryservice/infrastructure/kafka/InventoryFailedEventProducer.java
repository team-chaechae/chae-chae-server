package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.infrastructure.kafka.dto.InventoryFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 재고 차감 실패 이벤트 Producer
 * inventory-failed 토픽으로 발행하여 payment-service에서 환불 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedEventProducer {

    private static final String TOPIC = "inventory-failed";
    private static final String EVENT_TYPE = "INVENTORY_FAILED";

    private final KafkaTemplate<String, Object> objectKafkaTemplate;

    /**
     * 즉시 발행
     */
    public void publish(InventoryFailedEvent event) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        try {
            CompletableFuture<?> sendFuture = objectKafkaTemplate.send(TOPIC, orderId, event);
            sendFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[Kafka] 재고 차감 실패 이벤트 발행 - orderId: {}, salesId: {}, reason: {}",
                            orderId, salesId, event.getReason());
                } else {
                    log.warn("[Kafka] 재고 차감 실패 이벤트 발행 실패 - orderId: {}, salesId: {}, error: {}",
                            orderId, salesId, ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("[Kafka] 재고 차감 실패 이벤트 발행 실패 - orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getMessage());
        }
    }
}
