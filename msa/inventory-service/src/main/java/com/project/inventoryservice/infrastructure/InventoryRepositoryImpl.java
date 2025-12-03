package com.project.inventoryservice.infrastructure;

import com.project.inventoryservice.application.response.InventoryWithProductDto;
import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final JpaInventoryRepository jpaInventoryRepository;
    private final InventoryQueryRepository inventoryQueryRepository;
    private final JdbcInventoryRepository jdbcInventoryRepository;

    @Override
    public InventoryEntity save(InventoryEntity inventoryEntity) {
        return jpaInventoryRepository.save(inventoryEntity);
    }

    @Override
    public List<InventoryEntity> saveAll(List<InventoryEntity> inventoryEntities) {
        jdbcInventoryRepository.bulkInsert(inventoryEntities);
        return inventoryEntities;
    }

    @Override
    public InventoryEntity findInventoryByInventoryId(Long id) {
        return jpaInventoryRepository.findByIdAndDeletedAtIsNull(id).orElseThrow();
    }

    @Override
    public InventoryWithProductDto findInventoryWithProductById(Long id) {
        return inventoryQueryRepository.findInventoryWithProductById(id);
    }

    @Override
    public Page<InventoryWithProductDto> findInventoryWithProduct(
        Pageable pageable,
        String productName,
        Boolean deletedAt,
        String productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList) {
        return inventoryQueryRepository.findInventoryWithProduct(
            pageable, productName, deletedAt, productStatus, productId,
            startDate, endDate, exactDate, sortList);
    }

    @Override
    public Integer getCurrentStock(Long productId) {
        return inventoryQueryRepository.getCurrentStock(productId);
    }

    @Override
    public Map<Long, Integer> getCurrentStockMap(List<Long> productIds) {
        return inventoryQueryRepository.getCurrentStockMap(productIds);
    }

}
