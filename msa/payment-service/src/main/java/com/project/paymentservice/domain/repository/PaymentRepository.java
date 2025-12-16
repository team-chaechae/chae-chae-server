package com.project.paymentservice.domain.repository;

import com.project.paymentservice.domain.model.PaymentEntity;

import java.util.Optional;

public interface PaymentRepository {

    PaymentEntity save(PaymentEntity payment);

    Optional<PaymentEntity> findById(Long id);

    Optional<PaymentEntity> findBySalesId(Long salesId);
}
