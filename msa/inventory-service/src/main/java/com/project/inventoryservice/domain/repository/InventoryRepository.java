package com.project.inventoryservice.domain.repository;

import com.project.inventoryservice.application.response.InventoryWithProductDto;
import com.project.inventoryservice.domain.model.InventoryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface InventoryRepository {

    InventoryEntity save(InventoryEntity inventoryEntity);

    List<InventoryEntity> saveAll(List<InventoryEntity> inventoryEntities);

    InventoryEntity findInventoryByInventoryId(Long id);

    // 특정 Inventory를 Product 정보와 함께 조회
    InventoryWithProductDto findInventoryWithProductById(Long id);

    // Inventory와 Product 정보를 조인해서 조회 (Projection 사용)
    Page<InventoryWithProductDto> findInventoryWithProduct(
        Pageable pageable,
        String productName,
        Boolean deletedAt,
        String productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,

        List<String> sortList);

    // 현재 재고 조회 (히스토리 기반)
    Integer getCurrentStock(Long productId);

    // 여러 상품의 현재 재고 조회
    Map<Long, Integer> getCurrentStockMap(List<Long> productIds);

}
