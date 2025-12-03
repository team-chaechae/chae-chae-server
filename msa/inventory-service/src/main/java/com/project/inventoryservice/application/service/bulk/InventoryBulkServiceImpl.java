package com.project.inventoryservice.application.service.bulk;

import com.project.inventoryservice.application.global.annotation.BatchProcessing;
import com.project.inventoryservice.application.response.bulk.ResBulkCreateInventoryPostDTO;
import com.project.inventoryservice.application.response.bulk.ResBulkSaleInventoryDTO;
import com.project.inventoryservice.application.response.bulk.ResUpdateInventoryDTO;
import com.project.inventoryservice.application.service.InventoryCommonService;
import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import com.project.inventoryservice.presentation.request.bulk.ReqBulkCreateInventoryDTO;
import com.project.inventoryservice.presentation.request.bulk.ReqUpdateInventoryDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 물류/재고 관리 전용 서비스 구현
 *
 * 책임:
 * - 입고/출고 대량 처리 오케스트레이션
 * - 재고 조정
 * - 검증 및 배치 분할
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryBulkServiceImpl implements InventoryBulkService {

    private static final int BATCH_SIZE = 100;

    private final InventoryRepository inventoryRepository;
    private final InventoryBatchProcessor batchProcessor;
    private final InventoryCommonService inventoryCommonService;

    @Override
    @BatchProcessing(value = "대량 입고", batchSize = BATCH_SIZE)
    // @Transactional 제거 - 내부 REQUIRES_NEW와 충돌 방지
    public ResBulkCreateInventoryPostDTO createInventory(ReqBulkCreateInventoryDTO dto) {
        List<Long> productIds = dto.getProductIds();
        List<Integer> quantities = dto.getQuantities();

        // 상품 ID 유효성 검증
        inventoryCommonService.validateProductIds(productIds);

        // 배치 단위로 쪼개서 처리 (응용 계층 오케스트레이션)
        List<InventoryEntity> allResults = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, productIds.size());
            List<Long> batchIds = productIds.subList(i, endIndex);
            List<Integer> batchQuantities = quantities.subList(i, endIndex);

            log.info("배치 처리 중 - {}/{}", endIndex, productIds.size());

            // 도메인 서비스를 통한 배치 처리
            List<InventoryEntity> batchResults = batchProcessor.processBatchReceive(batchIds, batchQuantities);
            allResults.addAll(batchResults);
        }

        return ResBulkCreateInventoryPostDTO.from(allResults);
    }

    @Override
    @BatchProcessing(value = "재고 수정", batchSize = BATCH_SIZE)
    // @Transactional 제거 - 내부 REQUIRES_NEW와 충돌 방지
    public ResUpdateInventoryDTO modifyInventory(ReqUpdateInventoryDTO dto) {
        List<Long> productIds = dto.getProductIds();
        List<Integer> newQuantities = dto.getQuantities();

        // 상품 ID 유효성 검증
        inventoryCommonService.validateProductIds(productIds);

        // 1. 현재 재고 조회 (히스토리 기반)
        Map<Long, Integer> currentStockMap = inventoryRepository.getCurrentStockMap(productIds);

        // 2. 변경량 계산 (새 재고 - 현재 재고)
        List<Integer> changeAmounts = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer oldQuantity = currentStockMap.getOrDefault(productId, 0);
            Integer changeAmount = newQuantities.get(i) - oldQuantity;
            changeAmounts.add(changeAmount);
        }

        // 3. 배치 단위로 쪼개서 처리 (응용 계층 오케스트레이션)
        List<InventoryEntity> allResults = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, productIds.size());
            List<Long> batchIds = productIds.subList(i, endIndex);
            List<Integer> batchChangeAmounts = changeAmounts.subList(i, endIndex);

            log.info("배치 처리 중 - {}/{}", endIndex, productIds.size());

            // 도메인 서비스를 통한 배치 처리 (변경량 전달)
            List<InventoryEntity> batchResults = batchProcessor.processBatchModify(batchIds, batchChangeAmounts);
            allResults.addAll(batchResults);
        }

        return ResUpdateInventoryDTO.from(allResults);
    }

    @Override
    @BatchProcessing(value = "물류 출고", batchSize = BATCH_SIZE)
    @Transactional
    public ResBulkSaleInventoryDTO decreaseInventoryForWarehouseShipment(ReqUpdateInventoryDTO dto) {
        // DTO에서 데이터 추출
        List<Long> productIds = dto.getProductIds();
        List<Integer> quantities = dto.getQuantities();

        // 공통 서비스를 사용하여 재고 감소 처리
        List<InventoryEntity> savedHistories = inventoryCommonService.decreaseInventory(
            productIds,
            quantities,
            InventoryChangeType.ADJUST
        );

        return ResBulkSaleInventoryDTO.from(savedHistories);
    }
}
