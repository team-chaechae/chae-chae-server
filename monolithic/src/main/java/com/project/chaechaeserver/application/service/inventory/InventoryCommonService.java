package com.project.chaechaeserver.application.service.inventory;

import com.project.chaechaeserver.application.global.excepion.BadRequestException;
import com.project.chaechaeserver.application.global.excepion.EntityNotFoundException;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final ProductsRepository productsRepository;
    private final InventoryRepository inventoryRepository;

    /**
     * 상품 ID 유효성 검증 (최적화된 버전)
     *
     * @param productIds 검증할 상품 ID 목록
     * @throws EntityNotFoundException 존재하지 않는 상품이 있을 경우
     */
    public void validateProductIds(List<Long> productIds) {
        List<ProductEntity> foundProducts = productsRepository.findAllById(productIds);

        if (foundProducts.size() != productIds.size()) {
            // HashSet 사용으로 O(n) 복잡도로 개선
            Set<Long> foundIds = new HashSet<>();
            for (ProductEntity product : foundProducts) {
                foundIds.add(product.getId());
            }

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

        // 2. 현재 재고 조회
        Map<Long, Integer> currentStockMap = inventoryRepository.getCurrentStockMap(productIds);

        // 3. 재고 부족 검증
        validateStockAvailability(productIds, quantities, currentStockMap);

        // 4. 차감 히스토리 생성
        List<InventoryEntity> decreaseHistories = createDecreaseHistories(productIds, quantities, changeType);

        // 5. 히스토리 저장
        return inventoryRepository.saveAll(decreaseHistories);
    }
}