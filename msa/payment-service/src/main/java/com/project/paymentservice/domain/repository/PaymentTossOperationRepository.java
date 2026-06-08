package com.project.paymentservice.domain.repository;

import com.project.paymentservice.domain.model.PaymentTossOperationEntity;
import com.project.paymentservice.domain.model.PaymentTossOperationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTossOperationRepository extends JpaRepository<PaymentTossOperationEntity, Long> {

    Optional<PaymentTossOperationEntity> findByOperationId(String operationId);

    List<PaymentTossOperationEntity> findByStatus(PaymentTossOperationStatus status, Pageable pageable);
}
