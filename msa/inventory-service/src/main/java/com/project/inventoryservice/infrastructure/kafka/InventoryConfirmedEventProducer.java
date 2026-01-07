package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 재고 차감 성공 이벤트 Producer
 * inventory-confirmed 토픽으로 발행하여 order-service에서 SSE 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConfirmedEventProducer {

    private static final String TOPIC = "inventory-confirmed";
    private static final String EVENT_TYPE = "INVENTORY_CONFIRMED";

    private final KafkaTemplate<String, Object> objectKafkaTemplate;

    /**
     * 즉시 발행
     */
    public void publish(InventoryConfirmedEvent event) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        try {
            CompletableFuture<?> sendFuture = objectKafkaTemplate.send(TOPIC, orderId, event);
            sendFuture.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("[Kafka] 재고 차감 성공 이벤트 발행 - orderId: {}, salesId: {}",
                            orderId, salesId);
                } else {
                    log.warn("[Kafka] 재고 차감 성공 이벤트 발행 실패 - orderId: {}, salesId: {}, error: {}",
                            orderId, salesId, ex.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("[Kafka] 재고 차감 성공 이벤트 발행 실패 - orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getMessage());
        }
    }
}
