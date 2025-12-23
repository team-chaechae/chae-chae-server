package com.project.inventoryservice.application.scheduler;

import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.repository.StockRepository;
import com.project.inventoryservice.infrastructure.repository.InventoryQueryRepository;
import com.project.inventoryservice.infrastructure.repository.JpaInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * inventory 이벤트 스토어와 stock 스냅샷 테이블 동기화 스케줄러
 *
 * inventory 테이블: 이벤트 스토어 (Source of Truth)
 * stock 테이블: 현재 재고 스냅샷 (조회 성능 + 복구용)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncScheduler {

    private final StockRepository stockRepository;
    private final InventoryQueryRepository inventoryQueryRepository;
    private final JpaInventoryRepository jpaInventoryRepository;

    /**
     * 주기적으로 inventory 이벤트 스토어에서 stock 스냅샷 동기화
     * 매 5분마다 실행
     */
    @Scheduled(fixedRate = 300000) // 5분
    @Transactional
    public void syncStockFromInventory() {
        log.info("[Stock Sync] 동기화 시작");

        try {
            // 1. inventory 테이블에서 모든 고유 productId 조회
            List<Long> productIds = getAllProductIdsFromInventory();

            if (productIds.isEmpty()) {
                log.info("[Stock Sync] 동기화할 상품 없음");
                return;
            }

            // 2. inventory 이벤트 스토어에서 현재 재고 계산
            Map<Long, Integer> calculatedStockMap = inventoryQueryRepository.getCurrentStockMap(productIds);

            // 3. stock 테이블 업데이트
            int syncCount = 0;
            int mismatchCount = 0;

            for (Map.Entry<Long, Integer> entry : calculatedStockMap.entrySet()) {
                Long productId = entry.getKey();
                Integer calculatedStock = entry.getValue();

                StockEntity stockEntity = stockRepository.findByProductId(productId)
                        .orElse(null);

                if (stockEntity == null) {
                    // stock 테이블에 없으면 생성
                    stockRepository.save(new StockEntity(productId, calculatedStock));
                    syncCount++;
                    log.debug("[Stock Sync] 신규 생성 - productId: {}, quantity: {}", productId, calculatedStock);
                } else if (!stockEntity.getQuantity().equals(calculatedStock)) {
                    // 불일치 시 업데이트
                    int difference = calculatedStock - stockEntity.getQuantity();
                    if (difference > 0) {
                        stockEntity.increase(difference);
                    } else {
                        stockEntity.decrease(Math.abs(difference));
                    }
                    mismatchCount++;
                    log.warn("[Stock Sync] 불일치 보정 - productId: {}, stock: {} -> inventory: {}",
                            productId, stockEntity.getQuantity() - difference, calculatedStock);
                }
            }

            log.info("[Stock Sync] 동기화 완료 - 대상: {}개, 신규: {}개, 보정: {}개",
                    productIds.size(), syncCount, mismatchCount);

        } catch (Exception e) {
            log.error("[Stock Sync] 동기화 실패", e);
        }
    }

    /**
     * inventory 테이블에서 고유 productId 목록 조회
     */
    private List<Long> getAllProductIdsFromInventory() {
        return jpaInventoryRepository.findAllDistinctProductIds();
    }
}
