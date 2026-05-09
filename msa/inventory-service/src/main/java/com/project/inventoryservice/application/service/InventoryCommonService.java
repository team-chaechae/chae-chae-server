package com.project.inventoryservice.application.service;

import com.project.inventoryservice.application.global.exception.BadRequestException;
import com.project.inventoryservice.application.global.exception.EntityNotFoundException;
import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import com.project.inventoryservice.domain.repository.StockRepository;
import com.project.inventoryservice.infrastructure.client.ProductClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 재고 관련 공통 유틸리티 서비스
 *
 * 책임:
 * - 재고 검증 로직
 * - 재고 차감 생성
 * - 공통 유틸리티 기능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCommonService {

    private final ProductClient productClient;
    private final InventoryRepository inventoryRepository;
    private final StockRepository stockRepository;
    private final StockCacheService stockCacheService;

    /**
     * 상품 ID 유효성 검증 (MSA - Product Service 호출)
     *
     * @param productIds 검증할 상품 ID 목록
     * @throws EntityNotFoundException 존재하지 않는 상품이 있을 경우
     */
    public void validateProductIds(List<Long> productIds) {
        List<Long> validProductIds = productClient.validateProductIds(productIds);

        if (validProductIds.size() != productIds.size()) {
            // HashSet 사용으로 O(n) 복잡도로 개선
            Set<Long> foundIds = new HashSet<>(validProductIds);

            List<Long> missingIds = new ArrayList<>();
            for (Long id : productIds) {
                if (!foundIds.contains(id)) {
                    missingIds.add(id);
                }
            }

            if (!missingIds.isEmpty()) {
                throw new EntityNotFoundException(
                    String.format("다음 상품이 존재하지 않습니다: %s",
                        missingIds.toString().replaceAll("[\\[\\]]", ""))
                );
            }
        }
    }

    /**
     * 재고 가용성 검증
     *
     * @param productIds 상품 ID 목록
     * @param quantities 요청 수량 목록
     * @param currentStockMap 현재 재고 맵
     * @throws BadRequestException 재고가 부족한 경우 또는 리스트 크기가 다른 경우
     */
    public void validateStockAvailability(List<Long> productIds, List<Integer> quantities,
                                         Map<Long, Integer> currentStockMap) {
        // 리스트 크기 검증
        if (productIds.size() != quantities.size()) {
            throw new BadRequestException(
                String.format("상품 ID 개수와 수량 개수가 일치하지 않습니다. 상품ID: %d개, 수량: %d개",
                    productIds.size(), quantities.size())
            );
        }

        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer requestQuantity = quantities.get(i);
            Integer currentStock = currentStockMap.getOrDefault(productId, 0);

            if (currentStock < requestQuantity) {
                throw new BadRequestException(
                    String.format("재고가 부족합니다. 상품ID: %d, 현재재고: %d, 요청수량: %d",
                        productId, currentStock, requestQuantity)
                );
            }
        }
    }

    /**
     * 범용 재고 히스토리 생성 메서드
     *
     * @param productIds 상품 ID 목록
     * @param quantities 수량 목록
     * @param changeType 변경 타입
     * @return 생성된 재고 히스토리 목록
     * @throws BadRequestException 리스트 크기가 다른 경우
     */
    public List<InventoryEntity> createInventoryHistories(List<Long> productIds,
                                                          List<Integer> quantities,
                                                          InventoryChangeType changeType) {
        // 리스트 크기 검증
        if (productIds.size() != quantities.size()) {
            throw new BadRequestException(
                String.format("상품 ID 개수와 수량 개수가 일치하지 않습니다. 상품ID: %d개, 수량: %d개",
                    productIds.size(), quantities.size())
            );
        }

        List<InventoryEntity> histories = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            InventoryEntity history = InventoryEntity.builder()
                .productId(productIds.get(i))
                .quantity(quantities.get(i))
                .changeType(changeType)
                .build();
            histories.add(history);
        }

        return histories;
    }

    /**
     * 재고 차감 히스토리 생성 (음수 변환)
     *
     * @param productIds 상품 ID 목록
     * @param quantities 차감할 수량 목록 (양수로 전달, 내부에서 음수 변환)
     * @param changeType 변경 타입
     * @return 생성된 재고 히스토리 목록
     * @throws BadRequestException 리스트 크기가 다른 경우
     */
    public List<InventoryEntity> createDecreaseHistories(List<Long> productIds,
                                                         List<Integer> quantities,
                                                         InventoryChangeType changeType) {
        // 수량을 음수로 변환
        List<Integer> negativeQuantities = new ArrayList<>();
        for (Integer quantity : quantities) {
            negativeQuantities.add(-Math.abs(quantity));
        }

        return createInventoryHistories(productIds, negativeQuantities, changeType);
    }

    /**
     * 재고 감소 처리 (공통 로직)
     *
     * @param productIds 상품 ID 목록
     * @param quantities 차감할 수량 목록
     * @param changeType 변경 타입
     * @return 저장된 재고 히스토리
     */
    public List<InventoryEntity> decreaseInventory(List<Long> productIds,
                                                   List<Integer> quantities,
                                                   InventoryChangeType changeType) {
        // 1. 상품 존재 여부 검증
        validateProductIds(productIds);

        // 2. 현재 재고 조회 (stock 테이블에서 O(n) 조회)
        List<StockEntity> stocks = stockRepository.findByProductIdIn(productIds);
        Map<Long, Integer> currentStockMap = stocks.stream()
            .collect(Collectors.toMap(
                StockEntity::getProductId,
                StockEntity::getQuantity
            ));

        // 3. 재고 부족 검증
        validateStockAvailability(productIds, quantities, currentStockMap);

        // 4. 차감 히스토리 생성
        List<InventoryEntity> decreaseHistories = createDecreaseHistories(productIds, quantities, changeType);

        // 5. 히스토리 저장
        List<InventoryEntity> savedHistories = inventoryRepository.saveAll(decreaseHistories);

        // 6. stock 테이블 업데이트 + Redis 캐시 갱신
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer amount = quantities.get(i);

            // stock 테이블 업데이트
            stockRepository.decreaseStock(productId, Math.abs(amount));

            // 현재 재고 조회 후 Redis 캐시 갱신
            int currentStock = stockRepository.findByProductId(productId)
                .map(StockEntity::getQuantity)
                .orElse(0);
            stockCacheService.updateStockCache(productId, currentStock);
        }

        return savedHistories;
    }
}
