package com.project.deliveryservice.domain.repository;

import com.project.deliveryservice.domain.model.DeliveryCancellationRequestEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryCancellationRequestRepository extends JpaRepository<DeliveryCancellationRequestEntity, Long> {

    boolean existsBySalesId(Long salesId);

    Optional<DeliveryCancellationRequestEntity> findBySalesId(Long salesId);
}
