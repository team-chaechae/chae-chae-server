package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.service.StockCacheService;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryReserveEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 재고 차감 요청 Consumer (Order → Inventory)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReserveConsumer {

    private final StockCacheService stockCacheService;
    private final InventoryResultProducer resultProducer;
    private final InventoryEventProducer inventoryEventProducer;

    @KafkaListener(
            topics = "inventory-reserve",
            groupId = "inventory-saga-group",
            containerFactory = "inventoryReserveListenerFactory"
    )
    public void consumeReserveEvent(InventoryReserveEvent event) {
        log.info("[Kafka 수신] 재고 차감 요청 - salesId: {}, 상품 수: {}",
                event.getSalesId(), event.getItems().size());

        List<InventoryResultEvent.ItemResult> results = new ArrayList<>();
        String failureReason = null;

        try {
            // 모든 상품의 재고 차감 시도
            for (InventoryReserveEvent.Item item : event.getItems()) {
                Integer currentStock = stockCacheService.decreaseStock(
                        item.getProductId(),
                        item.getQuantity()
                );

                results.add(InventoryResultEvent.ItemResult.builder()
                        .productId(item.getProductId())
                        .currentStock(currentStock)
                        .build());

                // RDB 동기화를 위해 inventory-events 토픽으로 발행
                InventoryEvent inventoryEvent = InventoryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .productId(item.getProductId())
                        .quantity(-item.getQuantity())  // 감소는 음수로
                        .changeType("ORDER_DECREASE")
                        .occurredAt(LocalDateTime.now())
                        .currentStock(currentStock)
                        .build();
                inventoryEventProducer.publish(inventoryEvent);

                log.info("[재고 차감] productId: {}, 차감량: {}, 현재 재고: {}",
                        item.getProductId(), item.getQuantity(), currentStock);
            }

            // 성공 응답
            InventoryResultEvent resultEvent = InventoryResultEvent.builder()
                    .salesId(event.getSalesId())
                    .status(InventoryResultEvent.ResultStatus.SUCCESS)
                    .results(results)
                    .build();

            resultProducer.publishResultEvent(resultEvent);

        } catch (Exception e) {
            log.error("[재고 차감 실패] salesId: {}, error: {}", event.getSalesId(), e.getMessage());

            // 이미 차감한 재고 롤백
            rollbackReservedStock(event.getItems(), results.size());

            // 실패 응답
            InventoryResultEvent resultEvent = InventoryResultEvent.builder()
                    .salesId(event.getSalesId())
                    .status(InventoryResultEvent.ResultStatus.FAILED)
                    .failureReason(e.getMessage())
                    .build();

            resultProducer.publishResultEvent(resultEvent);
        }
    }

    /**
     * 재고 롤백 (보상 트랜잭션)
     */
    private void rollbackReservedStock(List<InventoryReserveEvent.Item> items, int successCount) {
        for (int i = 0; i < successCount; i++) {
            InventoryReserveEvent.Item item = items.get(i);
            try {
                Integer currentStock = stockCacheService.increaseStock(item.getProductId(), item.getQuantity());

                // RDB 동기화를 위해 inventory-events 토픽으로 발행 (롤백)
                InventoryEvent inventoryEvent = InventoryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())  // 증가는 양수로
                        .changeType("ORDER_RESTORE")
                        .occurredAt(LocalDateTime.now())
                        .currentStock(currentStock)
                        .build();
                inventoryEventProducer.publish(inventoryEvent);

                log.info("[재고 롤백] productId: {}, 복구량: {}", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("[재고 롤백 실패] productId: {}, error: {}", item.getProductId(), e.getMessage());
            }
        }
    }
}
