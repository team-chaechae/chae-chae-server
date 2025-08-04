package com.project.chaechaeserver.infrastructure.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final JpaInventoryRepository jpaInventoryRepository;
    private final InventoryQueryRepository inventoryQueryRepository;

    @Override
    public InventoryEntity save(InventoryEntity inventoryEntity) {
        return jpaInventoryRepository.save(inventoryEntity);
    }

    @Override
    public List<InventoryEntity> saveAll(List<InventoryEntity> inventoryEntities) {
        return jpaInventoryRepository.saveAll(inventoryEntities);
    }

    @Override
    public InventoryEntity findInventoryByInventoryId(Long id) {
        return jpaInventoryRepository.findByIdAndDeletedAtIsNull(id).orElseThrow();
    }

    @Override
    public Page<InventoryEntity> findInventoryByDeletedAtIsNullWithCondition(Pageable pageable,
        String productName, Boolean deletedAt, ProductStatusType productStatus, Long productId,
        LocalDate startDate, LocalDate endDate, LocalDate exactDate, List<String> sortList) {
        return inventoryQueryRepository.findInventoryWithCondition(
            pageable,productName, deletedAt, productStatus, productId, startDate, endDate, exactDate, sortList);
    }

}