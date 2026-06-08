package com.project.orderservice.domain.repository;

import com.project.orderservice.domain.model.PaymentOrchestrationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrchestrationRepository extends JpaRepository<PaymentOrchestrationEntity, Long> {

    Optional<PaymentOrchestrationEntity> findBySalesId(Long salesId);
}
