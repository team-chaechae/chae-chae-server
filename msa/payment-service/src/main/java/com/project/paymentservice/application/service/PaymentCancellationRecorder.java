package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.application.global.exception.BadRequestException;
import com.project.paymentservice.application.global.exception.NotFoundException;
import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.model.PaymentStatus;
import com.project.paymentservice.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancellationRecorder {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ResPaymentDTO recordTossPaymentCanceled(Long salesId, String reason) {
        ResPaymentDTO response = recordRefundedPayment(salesId, reason);
        log.info("[Payment] 토스페이먼츠 결제 취소 완료 - salesId: {}, reason: {}", salesId, reason);
        return response;
    }

    @Transactional
    public ResPaymentDTO recordPaymentRefunded(Long salesId, String reason) {
        return recordRefundedPayment(salesId, reason);
    }

    private ResPaymentDTO recordRefundedPayment(Long salesId, String reason) {
        PaymentEntity payment = paymentRepository.findBySalesId(salesId)
                .orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다. salesId: " + salesId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return ResPaymentDTO.from(payment);
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("완료된 결제만 취소할 수 있습니다. 현재 상태: " + payment.getStatus());
        }

        payment.refund(reason);
        eventPublisher.publishEvent(PaymentRefundedInternalEvent.of(payment.getOrderId(), salesId, reason));

        log.info("[Payment] 결제 환불 기록 완료 - orderId: {}, salesId: {}, reason: {}",
                payment.getOrderId(), salesId, reason);

        return ResPaymentDTO.from(payment);
    }
}
