package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompletionRecorder {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ResPaymentDTO recordTossPaymentCompleted(
            String orderId,
            Long salesId,
            Integer amount,
            String tossPaymentKey,
            String paymentMethod,
            LocalDateTime approvedAt,
            List<PaymentCompletedInternalEvent.OrderItem> items
    ) {
        return paymentRepository.findBySalesId(salesId)
                .map(ResPaymentDTO::from)
                .orElseGet(() -> createTossPaymentCompleted(
                        orderId,
                        salesId,
                        amount,
                        tossPaymentKey,
                        paymentMethod,
                        approvedAt,
                        items
                ));
    }

    @Transactional
    public void recordTossPaymentFailed(
            String orderId,
            Long salesId,
            Integer amount,
            String tossPaymentKey,
            String reason
    ) {
        if (paymentRepository.findBySalesId(salesId).isPresent()) {
            return;
        }

        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.assignTossPaymentKey(tossPaymentKey);
        payment.fail(reason);
        paymentRepository.save(payment);

        log.info("[Payment] 토스페이먼츠 결제 실패 기록 - orderId: {}, salesId: {}, reason: {}",
                orderId, salesId, reason);
    }

    private ResPaymentDTO createTossPaymentCompleted(
            String orderId,
            Long salesId,
            Integer amount,
            String tossPaymentKey,
            String paymentMethod,
            LocalDateTime approvedAt,
            List<PaymentCompletedInternalEvent.OrderItem> items
    ) {
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.completeWithToss(tossPaymentKey, paymentMethod, approvedAt);
        PaymentEntity savedPayment = paymentRepository.save(payment);

        eventPublisher.publishEvent(PaymentCompletedInternalEvent.of(orderId, salesId, amount, items));

        log.info("[Payment] 토스페이먼츠 결제 승인 완료 - orderId: {}, salesId: {}, amount: {}, method: {}",
                orderId, salesId, amount, paymentMethod);

        return ResPaymentDTO.from(savedPayment);
    }
}
