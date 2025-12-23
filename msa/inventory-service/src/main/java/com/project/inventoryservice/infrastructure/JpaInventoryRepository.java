package com.project.inventoryservice.infrastructure;

import com.project.inventoryservice.domain.model.InventoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInventoryRepository extends JpaRepository<InventoryEntity, Long> {


    Optional<InventoryEntity> findByIdAndDeletedAtIsNull (Long id);
}
