package com.project.chaechaeserver.domain.repository.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {
}
