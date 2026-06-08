package com.project.orderservice.application.service;

import com.project.orderservice.domain.model.PaymentOrchestrationEntity;
import com.project.orderservice.domain.repository.PaymentOrchestrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrationStateService {

    private final PaymentOrchestrationRepository orchestrationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity getOrCreate(String orderId, Long salesId) {
        return orchestrationRepository.findBySalesId(salesId)
                .orElseGet(() -> createOrLoadExisting(orderId, salesId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity markInventoryDeducted(PaymentOrchestrationEntity orchestration) {
        orchestration.markInventoryDeducted();
        return orchestrationRepository.save(orchestration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity markOrderCompleted(PaymentOrchestrationEntity orchestration) {
        orchestration.markOrderCompleted();
        return orchestrationRepository.save(orchestration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity startCompensation(
            PaymentOrchestrationEntity orchestration,
            String reason
    ) {
        orchestration.startCompensation(reason);
        return orchestrationRepository.save(orchestration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity markInventoryRestored(PaymentOrchestrationEntity orchestration) {
        orchestration.markInventoryRestored();
        return orchestrationRepository.save(orchestration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity markPaymentRefunded(PaymentOrchestrationEntity orchestration) {
        orchestration.markPaymentRefunded();
        return orchestrationRepository.save(orchestration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentOrchestrationEntity markOrderCancelled(PaymentOrchestrationEntity orchestration) {
        orchestration.markOrderCancelled();
        return orchestrationRepository.save(orchestration);
    }

    private PaymentOrchestrationEntity createOrLoadExisting(String orderId, Long salesId) {
        try {
            return orchestrationRepository.saveAndFlush(PaymentOrchestrationEntity.start(orderId, salesId));
        } catch (DataIntegrityViolationException e) {
            return orchestrationRepository.findBySalesId(salesId)
                    .orElseThrow(() -> e);
        }
    }
}
