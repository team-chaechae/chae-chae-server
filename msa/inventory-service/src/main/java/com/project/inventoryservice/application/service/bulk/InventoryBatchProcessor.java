package com.project.inventoryservice.application.service.bulk;

import com.project.inventoryservice.application.service.InventoryCommonService;
import com.project.inventoryservice.application.service.StockCacheService;
import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import com.project.inventoryservice.domain.repository.StockRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 배치 처리 전용 서비스
 *
 * <p>배치 단위로 재고를 처리하며, 각 배치는 독립적인 트랜잭션으로 실행됩니다.
 * REQUIRES_NEW 전파 레벨을 사용하여 배치별 부분 성공을 지원합니다.
 *
 * <p>설계 배경:
 * - 스프링 AOP 프록시는 같은 클래스 내부 메서드 호출 시 적용되지 않음
 * - REQUIRES_NEW 전파가 작동하려면 별도 빈으로 분리 필요
 * - 단일 책임 원칙: 배치 실행 로직만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryBatchProcessor {

    private final InventoryRepository inventoryRepository;
    private final StockRepository stockRepository;
    private final StockCacheService stockCacheService;
    private final InventoryCommonService inventoryCommonService;

    /**
     * 재고 입고 배치 처리 (독립 트랜잭션)
     *
     * @param batchIds 배치 상품 ID 리스트
     * @param batchQuantities 배치 입고 수량 리스트
     * @return 생성된 인벤토리 히스토리 리스트
     */
    @Retryable(
        retryFor = {CannotAcquireLockException.class, DeadlockLoserDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<InventoryEntity> processBatchReceive(List<Long> batchIds, List<Integer> batchQuantities) {
        log.info("배치 처리 시작 - batchIds: {}, 시도 시각: {}", batchIds, System.currentTimeMillis());

        // 1. 인벤토리 히스토리 생성 (입고 = 양수) - 공통 서비스 활용
        List<InventoryEntity> inventories = inventoryCommonService.createInventoryHistories(
            batchIds, batchQuantities, InventoryChangeType.RECEIVE
        );

        // 2. JDBC 벌크 인서트 (히스토리 저장)
        inventoryRepository.saveAll(inventories);

        // 3. stock 테이블 업데이트 + Redis 캐시 갱신
        for (int i = 0; i < batchIds.size(); i++) {
            Long productId = batchIds.get(i);
            Integer amount = batchQuantities.get(i);

            // stock 테이블 +amount (없으면 생성)
            int updated = stockRepository.increaseStock(productId, amount);
            if (updated == 0) {
                stockRepository.save(new StockEntity(productId, amount));
            }

            // 현재 재고 조회 후 Redis 캐시 갱신
            int currentStock = stockRepository.findByProductId(productId)
                .map(StockEntity::getQuantity)
                .orElse(0);
            stockCacheService.updateStockCache(productId, currentStock);
        }

        return inventories;
    }

    /**
     * 재고 수정 배치 처리 (독립 트랜잭션)
     *
     * @param batchIds 배치 상품 ID 리스트
     * @param batchChangeAmounts 배치 재고 변경량 리스트 (delta)
     * @return 생성된 인벤토리 히스토리 리스트
     */
    @Retryable(
        retryFor = {CannotAcquireLockException.class, DeadlockLoserDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<InventoryEntity> processBatchModify(
        List<Long> batchIds,
        List<Integer> batchChangeAmounts
    ) {
        log.info("배치 수정 시작 - batchIds: {}, 시도 시각: {}", batchIds, System.currentTimeMillis());

        // 1. 재고 조정 히스토리 생성 (변경량 delta) - 공통 서비스 활용
        List<InventoryEntity> modifyHistories = inventoryCommonService.createInventoryHistories(
            batchIds, batchChangeAmounts, InventoryChangeType.ADJUST
        );

        // 2. JDBC 벌크 인서트 (히스토리 저장)
        inventoryRepository.saveAll(modifyHistories);

        // 3. stock 테이블 업데이트 + Redis 캐시 갱신
        for (int i = 0; i < batchIds.size(); i++) {
            Long productId = batchIds.get(i);
            Integer changeAmount = batchChangeAmounts.get(i);

            if (changeAmount > 0) {
                // 증가
                int updated = stockRepository.increaseStock(productId, changeAmount);
                if (updated == 0) {
                    stockRepository.save(new StockEntity(productId, changeAmount));
                }
            } else if (changeAmount < 0) {
                // 감소
                stockRepository.decreaseStock(productId, Math.abs(changeAmount));
            }
            // changeAmount == 0 이면 아무것도 안함

            // 현재 재고 조회 후 Redis 캐시 갱신
            if (changeAmount != 0) {
                int currentStock = stockRepository.findByProductId(productId)
                    .map(StockEntity::getQuantity)
                    .orElse(0);
                stockCacheService.updateStockCache(productId, currentStock);
            }
        }

        return modifyHistories;
    }
}
