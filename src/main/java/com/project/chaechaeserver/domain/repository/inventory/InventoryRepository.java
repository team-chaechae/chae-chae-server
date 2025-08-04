package com.project.chaechaeserver.domain.repository.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface InventoryRepository {

    InventoryEntity save(InventoryEntity inventoryEntity);

    List<InventoryEntity> saveAll(List<InventoryEntity> inventoryEntities);

    InventoryEntity findInventoryByInventoryId(Long id);

    Page<InventoryEntity> findInventoryByDeletedAtIsNullWithCondition(
        Pageable pageable,
        String productName,
        Boolean deletedAt,
        ProductStatusType productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList);

}
