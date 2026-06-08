package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.global.exception.BadRequestException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl 단위 테스트")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private PaymentCompletionRecorder paymentCompletionRecorder;

    @Mock
    private PaymentCancellationRecorder paymentCancellationRecorder;

    @Mock
    private OrderSalesClient orderSalesClient;

    @Mock
    private PaymentTossOperationService paymentTossOperationService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private String orderId;
    private Long salesId;
    private Integer amount;

    @BeforeEach
    void setUp() {
        orderId = "order-123";
        salesId = 1L;
        amount = 50000;
    }

    @Test
    @DisplayName("신규 결제 요청 시 결제가 정상적으로 생성된다")
    void processPayment_NewPayment_Success() {
        // given
        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.empty());
        given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPayment().getSalesId()).isEqualTo(salesId);
        assertThat(result.getPayment().getAmount()).isEqualTo(amount);
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());

        verify(paymentRepository).findBySalesId(salesId);
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(eventPublisher).publishEvent(any(PaymentCompletedInternalEvent.class));
    }

    @Test
    @DisplayName("중복 결제 요청 시 기존 결제 정보를 반환한다")
    void processPayment_DuplicatePayment_ReturnsExisting() {
        // given
        PaymentEntity existingPayment = PaymentEntity.create(orderId, salesId, amount);
        existingPayment.process();
        existingPayment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(existingPayment));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPayment().getSalesId()).isEqualTo(salesId);
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());

        verify(paymentRepository).findBySalesId(salesId);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("중복 결제 요청 시 다른 금액이어도 기존 결제 정보를 반환한다")
    void processPayment_DuplicateWithDifferentAmount_ReturnsExisting() {
        // given
        PaymentEntity existingPayment = PaymentEntity.create(orderId, salesId, 30000);
        existingPayment.process();
        existingPayment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(existingPayment));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, 50000);

        // then
        assertThat(result.getPayment().getAmount()).isEqualTo(30000);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("실패한 결제가 있어도 기존 결제 정보를 반환한다")
    void processPayment_ExistingFailedPayment_ReturnsExisting() {
        // given
        PaymentEntity failedPayment = PaymentEntity.create(orderId, salesId, amount);
        failedPayment.process();
        failedPayment.fail("카드 한도 초과");

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(failedPayment));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.FAILED.name());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("토스페이먼츠 결제 승인 성공 시 승인 API 호출 후 결제 완료를 기록한다")
    void confirmTossPayment_Success() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        TossPaymentConfirmResponse tossResponse = new TossPaymentConfirmResponse(
                paymentKey,
                orderId,
                "DONE",
                amount,
                "카드",
                OffsetDateTime.now()
        );
        PaymentEntity completedPayment = PaymentEntity.create(orderId, salesId, amount);
        completedPayment.process();
        completedPayment.completeWithToss(paymentKey, "카드", LocalDateTime.now());
        ResPaymentDTO expectedResponse = ResPaymentDTO.from(completedPayment);

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.empty());
        given(orderSalesClient.getSales(salesId)).willReturn(testSalesDetail());
        given(tossPaymentClient.confirmPayment(paymentKey, orderId, amount, "payment-confirm-" + salesId))
                .willReturn(tossResponse);
        given(paymentCompletionRecorder.recordTossPaymentCompleted(
                eq(orderId),
                eq(salesId),
                eq(amount),
                eq(paymentKey),
                eq("카드"),
                any(LocalDateTime.class),
                anyList()
        )).willReturn(expectedResponse);

        // when
        ResPaymentDTO result = paymentService.confirmTossPayment(paymentKey, orderId, salesId, amount);

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());
        assertThat(result.getPayment().getTossPaymentKey()).isEqualTo(paymentKey);

        verify(tossPaymentClient).confirmPayment(paymentKey, orderId, amount, "payment-confirm-" + salesId);
        verify(paymentCompletionRecorder).recordTossPaymentCompleted(
                eq(orderId),
                eq(salesId),
                eq(amount),
                eq(paymentKey),
                eq("카드"),
                any(LocalDateTime.class),
                anyList()
        );
    }

    @Test
    @DisplayName("토스페이먼츠 중복 승인 요청 시 기존 결제 정보를 반환한다")
    void confirmTossPayment_Duplicate_ReturnsExisting() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        PaymentEntity existingPayment = PaymentEntity.create(orderId, salesId, amount);
        existingPayment.process();
        existingPayment.completeWithToss(paymentKey, "카드", LocalDateTime.now());

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(existingPayment));

        // when
        ResPaymentDTO result = paymentService.confirmTossPayment(paymentKey, orderId, salesId, amount);

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());
        assertThat(result.getPayment().getTossPaymentKey()).isEqualTo(paymentKey);
        verifyNoInteractions(tossPaymentClient);
        verifyNoInteractions(paymentCompletionRecorder);
    }

    @Test
    @DisplayName("토스페이먼츠 결제 승인 실패 시 실패 결제를 기록하고 토스 예외를 전파한다")
    void confirmTossPayment_TossFailure_RecordsFailureAndThrowsTossException() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.empty());
        given(orderSalesClient.getSales(salesId)).willReturn(testSalesDetail());
        given(tossPaymentClient.confirmPayment(paymentKey, orderId, amount, "payment-confirm-" + salesId))
                .willThrow(new TossPaymentException("REJECT_CARD_COMPANY", "카드사 승인 거절", 400));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmTossPayment(paymentKey, orderId, salesId, amount))
                .isInstanceOf(TossPaymentException.class)
                .hasMessage("카드사 승인 거절");

        verify(paymentCompletionRecorder).recordTossPaymentFailed(
                orderId,
                salesId,
                amount,
                paymentKey,
                "카드사 승인 거절"
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("토스 승인 성공 후 로컬 완료 기록 실패 시 복구 대상 operation으로 남긴다")
    void confirmTossPayment_WhenLocalRecordFailsAfterTossSuccess_MarksOperationForRecovery() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        TossPaymentConfirmResponse tossResponse = new TossPaymentConfirmResponse(
                paymentKey,
                orderId,
                "DONE",
                amount,
                "카드",
                OffsetDateTime.now()
        );

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.empty());
        given(orderSalesClient.getSales(salesId)).willReturn(testSalesDetail());
        given(tossPaymentClient.confirmPayment(paymentKey, orderId, amount, "payment-confirm-" + salesId))
                .willReturn(tossResponse);
        given(paymentCompletionRecorder.recordTossPaymentCompleted(
                eq(orderId),
                eq(salesId),
                eq(amount),
                eq(paymentKey),
                eq("카드"),
                any(LocalDateTime.class),
                anyList()
        )).willThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmTossPayment(paymentKey, orderId, salesId, amount))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");

        verify(paymentTossOperationService).beginConfirm(
                "payment-confirm-" + salesId,
                orderId,
                salesId,
                amount,
                paymentKey
        );
        verify(paymentTossOperationService).markConfirmTossSucceeded(
                eq("payment-confirm-" + salesId),
                eq("카드"),
                any(LocalDateTime.class)
        );
        verify(paymentTossOperationService).markLocalRecordFailed(
                eq("payment-confirm-" + salesId),
                any(RuntimeException.class)
        );
        verify(paymentTossOperationService, never()).markLocalRecorded("payment-confirm-" + salesId);
    }

    @Test
    @DisplayName("토스페이먼츠 결제 취소 성공 시 취소 API 호출 후 환불 상태를 기록한다")
    void cancelTossPayment_Success() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        String reason = "고객 요청";
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.completeWithToss(paymentKey, "카드", LocalDateTime.now());

        PaymentEntity refundedPayment = PaymentEntity.create(orderId, salesId, amount);
        refundedPayment.process();
        refundedPayment.completeWithToss(paymentKey, "카드", LocalDateTime.now());
        refundedPayment.refund(reason);
        ResPaymentDTO expectedResponse = ResPaymentDTO.from(refundedPayment);

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));
        given(tossPaymentClient.cancelPayment(paymentKey, reason, "payment-cancel-" + salesId))
                .willReturn(new TossPaymentCancelResponse(
                        paymentKey,
                        orderId,
                        "CANCELED",
                        amount,
                        "카드",
                        OffsetDateTime.now(),
                        List.of(new TossPaymentCancelResponse.CancelDetail(
                                amount,
                                reason,
                                0,
                                OffsetDateTime.now(),
                                "cancel_tx_key",
                                "DONE"
                        ))
                ));
        given(paymentCancellationRecorder.recordTossPaymentCanceled(salesId, reason))
                .willReturn(expectedResponse);

        // when
        ResPaymentDTO result = paymentService.cancelTossPayment(salesId, reason);

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED.name());
        verify(tossPaymentClient).cancelPayment(paymentKey, reason, "payment-cancel-" + salesId);
        verify(paymentCancellationRecorder).recordTossPaymentCanceled(salesId, reason);
    }

    @Test
    @DisplayName("토스 취소 성공 후 로컬 환불 기록 실패 시 복구 대상 operation으로 남긴다")
    void cancelTossPayment_WhenLocalRecordFailsAfterTossSuccess_MarksOperationForRecovery() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        String reason = "고객 요청";
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.completeWithToss(paymentKey, "카드", LocalDateTime.now());

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));
        given(tossPaymentClient.cancelPayment(paymentKey, reason, "payment-cancel-" + salesId))
                .willReturn(new TossPaymentCancelResponse(
                        paymentKey,
                        orderId,
                        "CANCELED",
                        amount,
                        "카드",
                        OffsetDateTime.now(),
                        List.of(new TossPaymentCancelResponse.CancelDetail(
                                amount,
                                reason,
                                0,
                                OffsetDateTime.now(),
                                "cancel_tx_key",
                                "DONE"
                        ))
                ));
        given(paymentCancellationRecorder.recordTossPaymentCanceled(salesId, reason))
                .willThrow(new RuntimeException("DB 장애"));

        // when & then
        assertThatThrownBy(() -> paymentService.cancelTossPayment(salesId, reason))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB 장애");

        verify(paymentTossOperationService).beginCancel(
                "payment-cancel-" + salesId,
                orderId,
                salesId,
                paymentKey,
                reason
        );
        verify(paymentTossOperationService).markCancelTossSucceeded("payment-cancel-" + salesId);
        verify(paymentTossOperationService).markLocalRecordFailed(
                eq("payment-cancel-" + salesId),
                any(RuntimeException.class)
        );
        verify(paymentTossOperationService, never()).markLocalRecorded("payment-cancel-" + salesId);
    }

    @Test
    @DisplayName("토스페이먼츠 결제 키가 없으면 토스 취소 요청을 보내지 않는다")
    void cancelTossPayment_MissingPaymentKey_ThrowsBadRequest() {
        // given
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> paymentService.cancelTossPayment(salesId, "고객 요청"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("토스페이먼츠 결제 키가 없는 결제는 토스 취소를 요청할 수 없습니다.");

        verifyNoInteractions(tossPaymentClient);
        verifyNoInteractions(paymentCancellationRecorder);
    }

    @Test
    @DisplayName("이미 환불된 토스페이먼츠 결제 취소 요청은 토스 API를 다시 호출하지 않고 기존 결제를 반환한다")
    void cancelTossPayment_AlreadyRefunded_ReturnsExistingWithoutTossCall() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        String reason = "고객 요청";
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.completeWithToss(paymentKey, "카드", LocalDateTime.now());
        payment.refund(reason);
        ResPaymentDTO expectedResponse = ResPaymentDTO.from(payment);

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));
        given(paymentCancellationRecorder.recordTossPaymentCanceled(salesId, reason))
                .willReturn(expectedResponse);

        // when
        ResPaymentDTO result = paymentService.cancelTossPayment(salesId, reason);

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED.name());
        verifyNoInteractions(tossPaymentClient);
        verify(paymentCancellationRecorder).recordTossPaymentCanceled(salesId, reason);
    }

    @Test
    @DisplayName("토스페이먼츠 결제 취소 실패 시 로컬 환불 상태를 기록하지 않는다")
    void cancelTossPayment_TossFailure_ThrowsBadRequest() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        String reason = "고객 요청";
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.completeWithToss(paymentKey, "카드", LocalDateTime.now());

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));
        given(tossPaymentClient.cancelPayment(paymentKey, reason, "payment-cancel-" + salesId))
                .willThrow(new TossPaymentException("ALREADY_CANCELED_PAYMENT", "이미 취소된 결제 입니다.", 400));

        // when & then
        assertThatThrownBy(() -> paymentService.cancelTossPayment(salesId, reason))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 취소된 결제 입니다.");

        verifyNoInteractions(paymentCancellationRecorder);
    }

    @Test
    @DisplayName("일반 결제 환불 처리 시 로컬 환불 기록을 위임한다")
    void refundPayment_DelegatesLocalRefundRecording() {
        // given
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));

        // when
        paymentService.refundPayment(salesId, "재고 차감 실패");

        // then
        verify(paymentCancellationRecorder).recordPaymentRefunded(salesId, "재고 차감 실패");
        verifyNoInteractions(tossPaymentClient);
    }

    @Test
    @DisplayName("토스페이먼츠 결제 환불 처리 시 토스 취소 API 성공 후 로컬 환불 기록을 위임한다")
    void refundPayment_TossPayment_CancelsTossBeforeRecordingRefund() {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        String reason = "재고 차감 실패";
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.completeWithToss(paymentKey, "카드", LocalDateTime.now());

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));
        given(tossPaymentClient.cancelPayment(paymentKey, reason, "payment-cancel-" + salesId))
                .willReturn(new TossPaymentCancelResponse(
                        paymentKey,
                        orderId,
                        "CANCELED",
                        amount,
                        "카드",
                        OffsetDateTime.now(),
                        List.of(new TossPaymentCancelResponse.CancelDetail(
                                amount,
                                reason,
                                0,
                                OffsetDateTime.now(),
                                "cancel_tx_key",
                                "DONE"
                        ))
                ));

        // when
        paymentService.refundPayment(salesId, reason);

        // then
        verify(tossPaymentClient).cancelPayment(paymentKey, reason, "payment-cancel-" + salesId);
        verify(paymentCancellationRecorder).recordTossPaymentCanceled(salesId, reason);
    }

    private OrderSalesResponse.SalesDetail testSalesDetail() {
        return new OrderSalesResponse.SalesDetail(
                salesId,
                orderId,
                "PENDING",
                List.of(new OrderSalesResponse.SalesItemDetail(
                        1L,
                        1999L,
                        "상품_1999",
                        1,
                        amount,
                        amount
                )),
                1,
                amount
        );
    }
}
