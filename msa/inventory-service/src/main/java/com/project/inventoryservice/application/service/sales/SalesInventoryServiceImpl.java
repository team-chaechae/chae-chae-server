package com.project.inventoryservice.application.service.sales;

import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO;
import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO.InventoryChangeResult;
import com.project.inventoryservice.application.service.StockCacheService;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.presentation.request.sales.ReqSalesInventoryDTO;
import com.project.inventoryservice.presentation.request.sales.ReqSalesInventoryDTO.SalesInventoryItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesInventoryServiceImpl implements SalesInventoryService {

    private final StockCacheService stockCacheService;
    private final InventoryEventProducer eventProducer;
    private final SalesInventoryIdempotencyService idempotencyService;

    @Override
    public ResSalesInventoryDTO increaseInventory(ReqSalesInventoryDTO dto) {
        return idempotencyService.execute(dto.getOperationId(), () -> doIncreaseInventory(dto));
    }

    private ResSalesInventoryDTO doIncreaseInventory(ReqSalesInventoryDTO dto) {
        List<InventoryChangeResult> results = new ArrayList<>();

        try {
            for (SalesInventoryItem item : dto.getItems()) {
                int amount = Math.abs(item.getQuantity());

                // 1. Redis INCRBY (Atomic, 즉시 반영)
                Integer currentStock = stockCacheService.increaseStock(item.getProductId(), amount);

                // 2. Kafka 이벤트 발행 (비동기 히스토리 저장)
                InventoryEvent event = InventoryEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .productId(item.getProductId())
                        .quantity(amount)
                        .changeType("ORDER_RESTORE")
                        .occurredAt(LocalDateTime.now())
                        .currentStock(currentStock)
                        .build();

                eventProducer.publish(event);

                results.add(InventoryChangeResult.builder()
                        .productId(item.getProductId())
                        .changedQuantity(amount)
                        .currentStock(currentStock)
                        .build());
            }

            return ResSalesInventoryDTO.success(results);

        } catch (Exception e) {
            log.error("[재고 증가 실패] 상품 수: {}, 에러: {}", dto.getItems().size(), e.getMessage());
            throw e;
        }
    }

    @Override
    public ResSalesInventoryDTO decreaseInventory(ReqSalesInventoryDTO dto) {
        return idempotencyService.execute(dto.getOperationId(), () -> doDecreaseInventory(dto));
    }

    private ResSalesInventoryDTO doDecreaseInventory(ReqSalesInventoryDTO dto) {
        log.debug("[Sales] 재고 차감 요청: {} 건", dto.getItems().size());

        List<InventoryChangeResult> results = new ArrayList<>();

        for (SalesInventoryItem item : dto.getItems()) {
            int amount = Math.abs(item.getQuantity());

            // 1. Redis DECRBY (Atomic, 즉시 반영 + 음수 재고 방지)
            Integer currentStock = stockCacheService.decreaseStock(item.getProductId(), amount);

            // 2. Kafka 이벤트 발행 (비동기 히스토리 저장)
            InventoryEvent event = InventoryEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .productId(item.getProductId())
                    .quantity(-amount)
                    .changeType("ORDER_DECREASE")
                    .occurredAt(LocalDateTime.now())
                    .currentStock(currentStock)
                    .build();

            eventProducer.publish(event);

            results.add(InventoryChangeResult.builder()
                    .productId(item.getProductId())
                    .changedQuantity(-amount)
                    .currentStock(currentStock)
                    .build());
        }

        return ResSalesInventoryDTO.success(results);
    }
}
