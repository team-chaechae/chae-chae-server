package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.infrastructure.kafka.dto.InventoryReserveEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 재고 이벤트 Producer (Order → Inventory)
 *
 * 현재 동기 HTTP 방식으로 전환되어 비활성화됨
 * 활성화: application.yml에 kafka.saga.enabled=true 설정
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kafka.saga.enabled", havingValue = "true", matchIfMissing = false)
public class InventoryEventProducer {

    private static final String TOPIC = "inventory-reserve";

    private final KafkaTemplate<String, InventoryReserveEvent> kafkaTemplate;

    public void publishReserveEvent(InventoryReserveEvent event) {
        String key = String.valueOf(event.getSalesId());

        log.info("[Kafka 발행] 재고 차감 요청 - salesId: {}, 상품 수: {}",
                event.getSalesId(), event.getItems().size());

        kafkaTemplate.send(TOPIC, key, event)
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
