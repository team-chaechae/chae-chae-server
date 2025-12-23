package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import com.project.inventoryservice.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 이벤트 Kafka Consumer
 * 재고 변경 이벤트를 받아서 DB에 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventConsumer {

    private final InventoryRepository inventoryRepository;
    private final StockRepository stockRepository;

    @KafkaListener(
        topics = "inventory-events",
        groupId = "inventory-consumer-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(InventoryEvent event, Acknowledgment ack) {
        String eventId = event.getEventId();
        Long productId = event.getProductId();

        try {
            log.info("[Kafka 수신] 재고 이벤트 - 상품ID: {}, 변경량: {}, 타입: {}, EventID: {}",
                productId, event.getQuantity(), event.getChangeType(), eventId);

            // 1. inventory 테이블에 히스토리 저장 (Event Store)
            InventoryEntity inventory = InventoryEntity.builder()
                .productId(productId)
                .quantity(event.getQuantity())
                .changeType(InventoryChangeType.valueOf(event.getChangeType()))
                .build();

            inventoryRepository.save(inventory);
            log.debug("[DB 저장] inventory 테이블 - 상품ID: {}, 변경량: {}", productId, event.getQuantity());

            // 2. stock 테이블 동기화 (백업용 Read Model)
            if (event.getQuantity() > 0) {
                // 증가
                int updated = stockRepository.increaseStock(productId, event.getQuantity());
                if (updated == 0) {
                    // stock 레코드가 없으면 생성
                    log.debug("[DB 생성] stock 테이블 신규 레코드 - 상품ID: {}, 재고: {}",
                        productId, event.getQuantity());
                    stockRepository.save(new StockEntity(productId, event.getQuantity()));
                } else {
                    log.debug("[DB 업데이트] stock 테이블 증가 - 상품ID: {}, 증가량: {}",
                        productId, event.getQuantity());
                }
            } else if (event.getQuantity() < 0) {
                // 감소
                int absQuantity = Math.abs(event.getQuantity());
                stockRepository.decreaseStock(productId, absQuantity);
                log.debug("[DB 업데이트] stock 테이블 감소 - 상품ID: {}, 감소량: {}",
                    productId, absQuantity);
            }

            log.info("[Kafka 처리 완료] 재고 이벤트 - 상품ID: {}, 현재 재고(Redis): {}, EventID: {}",
                productId, event.getCurrentStock(), eventId);

            // 수동 ACK
            ack.acknowledge();

        } catch (IllegalArgumentException e) {
            log.error("[Kafka 처리 실패 - 잘못된 파라미터] 상품ID: {}, EventID: {}, 변경 타입: {}, 에러: {}",
                productId, eventId, event.getChangeType(), e.getMessage());
            // 잘못된 데이터는 재시도하지 않고 ACK (DLQ로 보내는 것이 좋음)
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[Kafka 처리 실패 - DB 에러] 상품ID: {}, EventID: {}, 에러 타입: {}, 메시지: {}",
                productId, eventId, e.getClass().getSimpleName(), e.getMessage());
            log.error("[Kafka 처리 실패] 스택트레이스", e);

            // 에러 발생 시 ACK 하지 않음 → 재시도
            throw new RuntimeException("재고 이벤트 DB 저장 실패 - 상품ID: " + productId, e);
        }
    }
}
