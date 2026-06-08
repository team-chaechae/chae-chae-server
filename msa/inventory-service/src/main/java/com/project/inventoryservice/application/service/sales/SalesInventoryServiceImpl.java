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
        List<AppliedStockChange> appliedChanges = new ArrayList<>();

        try {
            for (SalesInventoryItem item : dto.getItems()) {
                int amount = Math.abs(item.getQuantity());

                // 1. Redis INCRBY (Atomic, 즉시 반영)
                Integer currentStock = stockCacheService.increaseStock(item.getProductId(), amount);
                appliedChanges.add(AppliedStockChange.increase(item.getProductId(), amount));

                // 2. Kafka 이벤트 발행 성공 확인 후 히스토리 저장 흐름으로 넘긴다.
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
            rollbackAppliedChanges(appliedChanges, e);
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
        List<AppliedStockChange> appliedChanges = new ArrayList<>();

        try {
            for (SalesInventoryItem item : dto.getItems()) {
                int amount = Math.abs(item.getQuantity());

                // 1. Redis DECRBY (Atomic, 즉시 반영 + 음수 재고 방지)
                Integer currentStock = stockCacheService.decreaseStock(item.getProductId(), amount);
                appliedChanges.add(AppliedStockChange.decrease(item.getProductId(), amount));

                // 2. Kafka 이벤트 발행 성공 확인 후 히스토리 저장 흐름으로 넘긴다.
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
        } catch (RuntimeException e) {
            rollbackAppliedChanges(appliedChanges, e);
            throw e;
        }
    }

    private void rollbackAppliedChanges(List<AppliedStockChange> appliedChanges, Exception cause) {
        for (int i = appliedChanges.size() - 1; i >= 0; i--) {
            AppliedStockChange change = appliedChanges.get(i);
            if (change.increased()) {
                rollbackIncreasedStock(change.productId(), change.amount(), cause);
            } else {
                rollbackDecreasedStock(change.productId(), change.amount(), cause);
            }
        }
    }

    private void rollbackIncreasedStock(Long productId, int amount, Exception cause) {
        try {
            stockCacheService.decreaseStock(productId, amount);
            log.warn("[재고 증가 이벤트 발행 실패 롤백 완료] productId: {}, rollbackQuantity: {}",
                    productId, amount);
        } catch (Exception rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
            log.error("[재고 증가 이벤트 발행 실패 롤백 실패] productId: {}, rollbackQuantity: {}, error: {}",
                    productId, amount, rollbackFailure.getMessage());
        }
    }

    private void rollbackDecreasedStock(Long productId, int amount, Exception cause) {
        try {
            stockCacheService.increaseStock(productId, amount);
            log.warn("[재고 차감 이벤트 발행 실패 롤백 완료] productId: {}, rollbackQuantity: {}",
                    productId, amount);
        } catch (Exception rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
            log.error("[재고 차감 이벤트 발행 실패 롤백 실패] productId: {}, rollbackQuantity: {}, error: {}",
                    productId, amount, rollbackFailure.getMessage());
        }
    }

    private record AppliedStockChange(Long productId, int amount, boolean increased) {

        private static AppliedStockChange increase(Long productId, int amount) {
            return new AppliedStockChange(productId, amount, true);
        }

        private static AppliedStockChange decrease(Long productId, int amount) {
            return new AppliedStockChange(productId, amount, false);
        }
    }
}
