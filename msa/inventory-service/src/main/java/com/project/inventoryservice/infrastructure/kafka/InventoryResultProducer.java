package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.infrastructure.kafka.dto.InventoryResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 재고 처리 결과 Producer (Inventory → Order)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryResultProducer {

    private static final String TOPIC = "inventory-result";

    private final KafkaTemplate<String, InventoryResultEvent> resultKafkaTemplate;

    public void publishResultEvent(InventoryResultEvent event) {
        String key = String.valueOf(event.getSalesId());

        log.info("[Kafka 발행] 재고 처리 결과 - salesId: {}, status: {}",
                event.getSalesId(), event.getStatus());

        resultKafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("[Kafka 발행 성공] salesId: {}, partition: {}, offset: {}",
                                event.getSalesId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("[Kafka 발행 실패] salesId: {}, error: {}",
                                event.getSalesId(), ex.getMessage());
                    }
                });
    }
}
