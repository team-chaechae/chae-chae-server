package com.project.chaechaeserver.infrastructure.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInventoryRepository extends JpaRepository<InventoryEntity, Long> {


    Optional<InventoryEntity> findByIdAndDeletedAtIsNull (Long id);
}




