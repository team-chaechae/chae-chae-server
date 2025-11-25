package com.project.chaechaeserver.domain.repository.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistoryEntity, Long> {
}
