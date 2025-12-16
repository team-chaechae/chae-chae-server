package com.project.paymentservice.infrastructure.repository;

import com.project.paymentservice.domain.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaPaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findBySalesId(Long salesId);
}
