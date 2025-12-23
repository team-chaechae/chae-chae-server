package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.infrastructure.repository.JdbcInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 재고 이벤트 Kafka Consumer (배치 처리)
 * 재고 변경 이벤트를 배치로 받아서 DB에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final JdbcInventoryRepository jdbcInventoryRepository;

    @KafkaListener(
        topics = "inventory-events",
        groupId = "inventory-consumer-group",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatch(List<InventoryEvent> events, Acknowledgment ack) {
        if (events.isEmpty()) {
            ack.acknowledge();
            return;
        }

        log.info("[Kafka 배치 수신] 재고 이벤트 {} 건", events.size());

        try {
            // 배치 처리 (Inventory INSERT + Stock UPDATE)
            jdbcInventoryRepository.batchProcess(events);

            // 수동 ACK
            ack.acknowledge();

            log.info("[Kafka 배치 처리 완료] {} 건", events.size());

        } catch (Exception e) {
            log.error("[Kafka 배치 처리 실패] {} 건, 에러: {}", events.size(), e.getMessage());
            log.error("[Kafka 배치 처리 실패] 스택트레이스", e);

            // 에러 발생 시 ACK 하지 않음 → 재시도
            throw new RuntimeException("재고 이벤트 배치 처리 실패", e);
        }
    }
}
