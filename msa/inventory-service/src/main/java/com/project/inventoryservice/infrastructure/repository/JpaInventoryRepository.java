package com.project.inventoryservice.infrastructure.repository;

import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaInventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByIdAndDeletedAtIsNull(Long id);

    /**
     * inventory 테이블에서 고유 productId 목록 조회
     */
    @Query("SELECT DISTINCT i.productId FROM InventoryEntity i WHERE i.deletedAt IS NULL")
    List<Long> findAllDistinctProductIds();

    boolean existsByOrderIdAndProductIdAndChangeType(
        String orderId,
        Long productId,
        InventoryChangeType changeType
    );
}
