package com.project.paymentservice.infrastructure.repository;

import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JpaPaymentRepository jpaPaymentRepository;

    @Override
    public PaymentEntity save(PaymentEntity payment) {
        return jpaPaymentRepository.save(payment);
    }

    @Override
    public Optional<PaymentEntity> findById(Long id) {
        return jpaPaymentRepository.findById(id);
    }

    @Override
    public Optional<PaymentEntity> findBySalesId(Long salesId) {
        return jpaPaymentRepository.findBySalesId(salesId);
    }
}
