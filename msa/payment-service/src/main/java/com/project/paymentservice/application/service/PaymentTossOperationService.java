package com.project.paymentservice.application.service;

import com.project.paymentservice.domain.model.PaymentTossOperationEntity;
import com.project.paymentservice.domain.repository.PaymentTossOperationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTossOperationService {

    private final PaymentTossOperationRepository operationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTossOperationEntity beginConfirm(
            String operationId,
            String orderId,
            Long salesId,
            Integer amount,
            String paymentKey
    ) {
        return operationRepository.findByOperationId(operationId)
                .orElseGet(() -> createConfirm(operationId, orderId, salesId, amount, paymentKey));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentTossOperationEntity beginCancel(
            String operationId,
            String orderId,
            Long salesId,
            String paymentKey,
            String reason
    ) {
        return operationRepository.findByOperationId(operationId)
                .orElseGet(() -> createCancel(operationId, orderId, salesId, paymentKey, reason));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markConfirmTossSucceeded(
            String operationId,
            String paymentMethod,
            LocalDateTime approvedAt
    ) {
        PaymentTossOperationEntity operation = getOperation(operationId);
        operation.markConfirmTossSucceeded(paymentMethod, approvedAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCancelTossSucceeded(String operationId) {
        PaymentTossOperationEntity operation = getOperation(operationId);
        operation.markCancelTossSucceeded();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markLocalRecorded(String operationId) {
        PaymentTossOperationEntity operation = getOperation(operationId);
        operation.markLocalRecorded();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markLocalRecordFailed(String operationId, Exception cause) {
        PaymentTossOperationEntity operation = getOperation(operationId);
        operation.markLocalRecordFailed(cause.getMessage());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String operationId, Exception cause) {
        PaymentTossOperationEntity operation = getOperation(operationId);
        operation.markFailed(cause.getMessage());
    }

    private PaymentTossOperationEntity createConfirm(
            String operationId,
            String orderId,
            Long salesId,
            Integer amount,
            String paymentKey
    ) {
        try {
            return operationRepository.saveAndFlush(
                    PaymentTossOperationEntity.confirm(operationId, orderId, salesId, amount, paymentKey)
            );
        } catch (DataIntegrityViolationException e) {
            return operationRepository.findByOperationId(operationId)
                    .orElseThrow(() -> e);
        }
    }

    private PaymentTossOperationEntity createCancel(
            String operationId,
            String orderId,
            Long salesId,
            String paymentKey,
            String reason
    ) {
        try {
            return operationRepository.saveAndFlush(
                    PaymentTossOperationEntity.cancel(operationId, orderId, salesId, paymentKey, reason)
            );
        } catch (DataIntegrityViolationException e) {
            return operationRepository.findByOperationId(operationId)
                    .orElseThrow(() -> e);
        }
    }

    private PaymentTossOperationEntity getOperation(String operationId) {
        return operationRepository.findByOperationId(operationId)
                .orElseThrow(() -> new IllegalStateException("토스 결제 작업을 찾을 수 없습니다. operationId: " + operationId));
    }
}
