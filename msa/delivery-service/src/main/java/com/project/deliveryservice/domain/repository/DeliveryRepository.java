package com.project.deliveryservice.domain.repository;

import com.project.deliveryservice.domain.model.DeliveryEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, Long> {

    boolean existsBySalesId(Long salesId);

    boolean existsByTrackingNumber(String trackingNumber);

    Optional<DeliveryEntity> findBySalesId(Long salesId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select delivery from DeliveryEntity delivery where delivery.id = :deliveryId")
    Optional<DeliveryEntity> findByIdForUpdate(@Param("deliveryId") Long deliveryId);
}
