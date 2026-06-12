package com.project.orderservice.domain.repository;

import com.project.orderservice.domain.model.SalesDeliveryStatusEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesDeliveryStatusRepository extends JpaRepository<SalesDeliveryStatusEntity, Long> {

    Optional<SalesDeliveryStatusEntity> findBySalesId(Long salesId);

    List<SalesDeliveryStatusEntity> findBySalesIdIn(Collection<Long> salesIds);
}
