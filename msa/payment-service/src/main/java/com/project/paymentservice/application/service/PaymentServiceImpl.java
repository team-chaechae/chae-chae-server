package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.application.global.exception.NotFoundException;
import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.repository.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ResPaymentDTO processPayment(String orderId, Long salesId, Integer amount) {
        return processPayment(orderId, salesId, amount, null);
    }

    @Override
    @Transactional
    public ResPaymentDTO processPayment(String orderId, Long salesId, Integer amount,
            java.util.List<PaymentCompletedInternalEvent.OrderItem> items) {
        // 중복 결제 방지 - 기존 결제가 있는지 확인
        Optional<PaymentEntity> existingPayment = paymentRepository.findBySalesId(salesId);
        if (existingPayment.isPresent()) {
            PaymentEntity payment = existingPayment.get();
            log.info("[Payment] 중복 결제 요청 - orderId: {}, salesId: {}, 기존 상태: {}", orderId, salesId, payment.getStatus());
            return ResPaymentDTO.from(payment);
        }

        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();

        // 외부 결제 API 호출 대신 바로 완료 처리 (시뮬레이션)
        payment.complete();

        PaymentEntity savedPayment = paymentRepository.save(payment);

        log.info("[Payment] 결제 완료 - orderId: {}, salesId: {}, amount: {}", orderId, salesId, amount);

        // 결제 완료 이벤트 발행 (Outbox 패턴 - 트랜잭션과 함께 기록)
        eventPublisher.publishEvent(PaymentCompletedInternalEvent.of(orderId, salesId, amount, items));

        return ResPaymentDTO.from(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public ResPaymentDTO getPaymentBySalesId(Long salesId) {
        PaymentEntity payment = paymentRepository.findBySalesId(salesId)
            .orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다"));

        return ResPaymentDTO.from(payment);

    }

    @Override
    @Transactional
    public void completePayment(Long salesId) {
        PaymentEntity payment = findPaymentOrThrow(salesId);
        payment.complete();
        log.info("[Payment] 결제 상태 완료로 변경 - salesId: {}", salesId);
    }

    @Override
    @Transactional
    public void failPayment(Long salesId, String reason) {
        PaymentEntity payment = findPaymentOrThrow(salesId);
        payment.fail(reason);
        log.info("[Payment] 결제 실패 - salesId: {}, reason: {}", salesId, reason);
    }

    @Override
    @Transactional
    public void cancelPayment(Long salesId, String reason) {
        PaymentEntity payment = findPaymentOrThrow(salesId);
        payment.cancel(reason);
        log.info("[Payment] 결제 취소 - salesId: {}, reason: {}", salesId, reason);
    }

    @Override
    @Transactional
    public void refundPayment(Long salesId, String reason) {
        PaymentEntity payment = findPaymentOrThrow(salesId);
        payment.refund(reason);

        // 환불 이벤트 발행 (Outbox 패턴 - 트랜잭션과 함께 기록)
        // orderId를 키로 사용하여 같은 주문의 이벤트가 같은 파티션으로 가도록 보장
        eventPublisher.publishEvent(PaymentRefundedInternalEvent.of(payment.getOrderId(), salesId));

        log.info("[Payment] 환불 처리 - orderId: {}, salesId: {}, reason: {}",
                payment.getOrderId(), salesId, reason);
    }

    private PaymentEntity findPaymentOrThrow(Long salesId) {
        return paymentRepository.findBySalesId(salesId)
                .orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다. salesId: " + salesId));
    }
}
