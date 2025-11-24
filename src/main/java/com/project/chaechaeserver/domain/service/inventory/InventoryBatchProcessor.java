package com.project.chaechaeserver.domain.service.inventory;

import com.project.chaechaeserver.application.service.inventory.InventoryCommonService;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
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

        // 2. JDBC 벌크 인서트 (히스토리 저장만 수행, Product는 변경 없음)
        inventoryRepository.saveAll(inventories);

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

        // 2. JDBC 벌크 인서트 (히스토리 저장만 수행, Product는 변경 없음)
        inventoryRepository.saveAll(modifyHistories);

        return modifyHistories;
    }
}