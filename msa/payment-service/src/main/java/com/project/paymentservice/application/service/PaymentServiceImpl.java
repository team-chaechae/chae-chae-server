package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.global.exception.BadRequestException;
import com.project.paymentservice.application.global.exception.NotFoundException;
import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.model.PaymentStatus;
import com.project.paymentservice.domain.repository.PaymentRepository;
import com.project.paymentservice.infrastructure.order.OrderSalesClient;
import com.project.paymentservice.infrastructure.order.dto.OrderSalesResponse;
import com.project.paymentservice.infrastructure.tosspayments.TossPaymentClient;
import com.project.paymentservice.infrastructure.tosspayments.TossPaymentException;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentCancelResponse;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentConfirmResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentCompletionRecorder paymentCompletionRecorder;
    private final PaymentCancellationRecorder paymentCancellationRecorder;
    private final OrderSalesClient orderSalesClient;
    private final PaymentTossOperationService paymentTossOperationService;

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
    public ResPaymentDTO confirmTossPayment(String paymentKey, String orderId, Long salesId, Integer amount) {
        Optional<PaymentEntity> existingPayment = paymentRepository.findBySalesId(salesId);
        if (existingPayment.isPresent()) {
            return handleExistingTossPayment(existingPayment.get(), orderId, salesId, amount);
        }

        OrderSalesResponse.SalesDetail sales = orderSalesClient.getSales(salesId);
        validateSalesForTossConfirm(orderId, salesId, amount, sales);
        List<PaymentCompletedInternalEvent.OrderItem> items = toPaymentCompletedItems(sales.items());
        String operationId = buildConfirmIdempotencyKey(salesId);
        paymentTossOperationService.beginConfirm(operationId, orderId, salesId, amount, paymentKey);

        try {
            TossPaymentConfirmResponse response = tossPaymentClient.confirmPayment(
                    paymentKey,
                    orderId,
                    amount,
                    operationId
            );
            validateTossConfirmResponse(paymentKey, orderId, amount, response);
            paymentTossOperationService.markConfirmTossSucceeded(
                    operationId,
                    response.method(),
                    toLocalDateTime(response.approvedAt())
            );

            try {
                ResPaymentDTO result = paymentCompletionRecorder.recordTossPaymentCompleted(
                        orderId,
                        salesId,
                        amount,
                        response.paymentKey(),
                        response.method(),
                        toLocalDateTime(response.approvedAt()),
                        items
                );
                paymentTossOperationService.markLocalRecorded(operationId);
                return result;
            } catch (RuntimeException e) {
                paymentTossOperationService.markLocalRecordFailed(operationId, e);
                throw e;
            }
        } catch (TossPaymentException e) {
            paymentCompletionRecorder.recordTossPaymentFailed(
                    orderId,
                    salesId,
                    amount,
                    paymentKey,
                    e.getMessage()
            );
            paymentTossOperationService.markFailed(operationId, e);
            log.warn("[Payment] 토스페이먼츠 결제 승인 실패 - orderId: {}, salesId: {}, code: {}, statusCode: {}",
                    orderId, salesId, e.getCode(), e.getStatusCode());
            throw e;
        }
    }

    @Override
    public ResPaymentDTO cancelTossPayment(Long salesId, String cancelReason) {
        PaymentEntity payment = findPaymentOrThrow(salesId);

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("[Payment] 토스페이먼츠 중복 취소 요청 - orderId: {}, salesId: {}, 기존 상태: {}",
                    payment.getOrderId(), salesId, payment.getStatus());
            return paymentCancellationRecorder.recordTossPaymentCanceled(salesId, cancelReason);
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("완료된 결제만 취소할 수 있습니다. 현재 상태: " + payment.getStatus());
        }
        if (payment.getTossPaymentKey() == null || payment.getTossPaymentKey().isBlank()) {
            throw new BadRequestException("토스페이먼츠 결제 키가 없는 결제는 토스 취소를 요청할 수 없습니다.");
        }

        String operationId = buildCancelIdempotencyKey(salesId);
        paymentTossOperationService.beginCancel(
                operationId,
                payment.getOrderId(),
                salesId,
                payment.getTossPaymentKey(),
                cancelReason
        );
        try {
            TossPaymentCancelResponse response = tossPaymentClient.cancelPayment(
                    payment.getTossPaymentKey(),
                    cancelReason,
                    operationId
            );
            validateTossCancelResponse(payment, response);
            paymentTossOperationService.markCancelTossSucceeded(operationId);

            try {
                ResPaymentDTO result = paymentCancellationRecorder.recordTossPaymentCanceled(salesId, cancelReason);
                paymentTossOperationService.markLocalRecorded(operationId);
                return result;
            } catch (RuntimeException e) {
                paymentTossOperationService.markLocalRecordFailed(operationId, e);
                throw e;
            }
        } catch (TossPaymentException e) {
            paymentTossOperationService.markFailed(operationId, e);
            log.warn("[Payment] 토스페이먼츠 결제 취소 실패 - orderId: {}, salesId: {}, code: {}, statusCode: {}",
                    payment.getOrderId(), salesId, e.getCode(), e.getStatusCode());
            throw new BadRequestException(e.getMessage());
        }
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
    public void refundPayment(Long salesId, String reason) {
        PaymentEntity payment = findPaymentOrThrow(salesId);

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("[Payment] 중복 환불 요청 - orderId: {}, salesId: {}, 기존 상태: {}",
                    payment.getOrderId(), salesId, payment.getStatus());
            return;
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("완료된 결제만 환불할 수 있습니다. 현재 상태: " + payment.getStatus());
        }

        if (StringUtils.hasText(payment.getTossPaymentKey())) {
            String operationId = buildCancelIdempotencyKey(salesId);
            paymentTossOperationService.beginCancel(
                    operationId,
                    payment.getOrderId(),
                    salesId,
                    payment.getTossPaymentKey(),
                    reason
            );
            try {
                TossPaymentCancelResponse response = tossPaymentClient.cancelPayment(
                        payment.getTossPaymentKey(),
                        reason,
                        operationId
                );
                validateTossCancelResponse(payment, response);
                paymentTossOperationService.markCancelTossSucceeded(operationId);
                try {
                    paymentCancellationRecorder.recordTossPaymentCanceled(salesId, reason);
                    paymentTossOperationService.markLocalRecorded(operationId);
                } catch (RuntimeException e) {
                    paymentTossOperationService.markLocalRecordFailed(operationId, e);
                    throw e;
                }
            } catch (TossPaymentException e) {
                paymentTossOperationService.markFailed(operationId, e);
                log.warn("[Payment] 토스페이먼츠 환불 취소 실패 - orderId: {}, salesId: {}, code: {}, statusCode: {}",
                        payment.getOrderId(), salesId, e.getCode(), e.getStatusCode());
                throw new BadRequestException(e.getMessage());
            }
            return;
        }

        paymentCancellationRecorder.recordPaymentRefunded(salesId, reason);
        log.info("[Payment] 환불 처리 - orderId: {}, salesId: {}, reason: {}",
                payment.getOrderId(), salesId, reason);
    }

    private PaymentEntity findPaymentOrThrow(Long salesId) {
        return paymentRepository.findBySalesId(salesId)
                .orElseThrow(() -> new NotFoundException("결제 정보를 찾을 수 없습니다. salesId: " + salesId));
    }

    private ResPaymentDTO handleExistingTossPayment(PaymentEntity payment, String orderId, Long salesId, Integer amount) {
        if (payment.getStatus() == PaymentStatus.COMPLETED
                && orderId.equals(payment.getOrderId())
                && amount.equals(payment.getAmount())) {
            log.info("[Payment] 토스페이먼츠 중복 승인 요청 - orderId: {}, salesId: {}, 기존 상태: {}",
                    orderId, salesId, payment.getStatus());
            return ResPaymentDTO.from(payment);
        }

        throw new BadRequestException("이미 처리된 결제 정보와 요청 값이 일치하지 않습니다. salesId: " + salesId);
    }

    private void validateTossConfirmResponse(
            String paymentKey,
            String orderId,
            Integer amount,
            TossPaymentConfirmResponse response
    ) {
        if (response == null) {
            throw new BadRequestException("토스페이먼츠 결제 승인 응답이 비어 있습니다.");
        }
        if (!paymentKey.equals(response.paymentKey())) {
            throw new BadRequestException("토스페이먼츠 결제 키가 요청과 일치하지 않습니다.");
        }
        if (!orderId.equals(response.orderId())) {
            throw new BadRequestException("토스페이먼츠 주문 ID가 요청과 일치하지 않습니다.");
        }
        if (!amount.equals(response.totalAmount())) {
            throw new BadRequestException("토스페이먼츠 결제 금액이 요청과 일치하지 않습니다.");
        }
        if (!"DONE".equals(response.status())) {
            throw new BadRequestException("토스페이먼츠 결제가 완료 상태가 아닙니다. status: " + response.status());
        }
    }

    private void validateTossCancelResponse(PaymentEntity payment, TossPaymentCancelResponse response) {
        if (response == null) {
            throw new BadRequestException("토스페이먼츠 결제 취소 응답이 비어 있습니다.");
        }
        if (!payment.getTossPaymentKey().equals(response.paymentKey())) {
            throw new BadRequestException("토스페이먼츠 결제 키가 요청과 일치하지 않습니다.");
        }
        if (!payment.getOrderId().equals(response.orderId())) {
            throw new BadRequestException("토스페이먼츠 주문 ID가 요청과 일치하지 않습니다.");
        }
        if (!payment.getAmount().equals(response.totalAmount())) {
            throw new BadRequestException("토스페이먼츠 결제 금액이 기존 결제와 일치하지 않습니다.");
        }
        if (!"CANCELED".equals(response.status())) {
            throw new BadRequestException("토스페이먼츠 결제가 취소 상태가 아닙니다. status: " + response.status());
        }
    }

    private void validateSalesForTossConfirm(
            String orderId,
            Long salesId,
            Integer amount,
            OrderSalesResponse.SalesDetail sales
    ) {
        if (!salesId.equals(sales.salesId())) {
            throw new BadRequestException("판매 ID가 요청과 일치하지 않습니다. salesId: " + salesId);
        }
        if (!orderId.equals(sales.orderId())) {
            throw new BadRequestException("주문 ID가 판매 정보와 일치하지 않습니다.");
        }
        if (!amount.equals(sales.totalPrice())) {
            throw new BadRequestException("결제 금액이 판매 금액과 일치하지 않습니다.");
        }
        if (sales.items() == null || sales.items().isEmpty()) {
            throw new BadRequestException("판매 상품 정보가 비어 있습니다. salesId: " + salesId);
        }
    }

    private List<PaymentCompletedInternalEvent.OrderItem> toPaymentCompletedItems(
            List<OrderSalesResponse.SalesItemDetail> salesItems
    ) {
        return salesItems.stream()
                .map(item -> PaymentCompletedInternalEvent.OrderItem.builder()
                        .productId(item.productId())
                        .productName(item.productName())
                        .quantity(item.quantity())
                        .price(item.price())
                        .build())
                .toList();
    }

    private String buildConfirmIdempotencyKey(Long salesId) {
        return "payment-confirm-" + salesId;
    }

    private String buildCancelIdempotencyKey(Long salesId) {
        return "payment-cancel-" + salesId;
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime approvedAt) {
        return approvedAt == null ? LocalDateTime.now() : approvedAt.toLocalDateTime();
    }
}
