package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.domain.model.PaymentTossOperationEntity;
import com.project.paymentservice.domain.model.PaymentTossOperationStatus;
import com.project.paymentservice.domain.repository.PaymentTossOperationRepository;
import com.project.paymentservice.infrastructure.order.OrderSalesClient;
import com.project.paymentservice.infrastructure.order.dto.OrderSalesResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentTossOperationRecoveryService")
class PaymentTossOperationRecoveryServiceTest {

    @Mock
    private PaymentTossOperationRepository operationRepository;

    @Mock
    private PaymentCompletionRecorder paymentCompletionRecorder;

    @Mock
    private PaymentCancellationRecorder paymentCancellationRecorder;

    @Mock
    private PaymentTossOperationService operationService;

    @Mock
    private OrderSalesClient orderSalesClient;

    @Test
    @DisplayName("Toss 승인 성공 후 로컬 기록 실패 operation을 결제 완료로 복구한다")
    void recoverTossSucceededOperations_WhenConfirmOperation_RecordsPaymentCompleted() {
        // given
        PaymentTossOperationRecoveryService recoveryService = service();
        PaymentTossOperationEntity operation = PaymentTossOperationEntity.confirm(
                "payment-confirm-7",
                "order-7",
                7L,
                45000,
                "tgen_7"
        );
        LocalDateTime approvedAt = LocalDateTime.now();
        operation.markConfirmTossSucceeded("카드", approvedAt);

        given(operationRepository.findByStatus(eq(PaymentTossOperationStatus.TOSS_SUCCEEDED), any(Pageable.class)))
                .willReturn(List.of(operation));
        given(orderSalesClient.getSales(7L)).willReturn(testSalesDetail());

        // when
        recoveryService.recoverTossSucceededOperations();

        // then
        verify(paymentCompletionRecorder).recordTossPaymentCompleted(
                eq("order-7"),
                eq(7L),
                eq(45000),
                eq("tgen_7"),
                eq("카드"),
                eq(approvedAt),
                anyList()
        );
        verify(operationService).markLocalRecorded("payment-confirm-7");
        verify(paymentCancellationRecorder, never()).recordTossPaymentCanceled(any(), any());
    }

    @Test
    @DisplayName("Toss 취소 성공 후 로컬 기록 실패 operation을 환불 완료로 복구한다")
    void recoverTossSucceededOperations_WhenCancelOperation_RecordsPaymentRefunded() {
        // given
        PaymentTossOperationRecoveryService recoveryService = service();
        PaymentTossOperationEntity operation = PaymentTossOperationEntity.cancel(
                "payment-cancel-7",
                "order-7",
                7L,
                "tgen_7",
                "고객 요청"
        );
        operation.markCancelTossSucceeded();

        given(operationRepository.findByStatus(eq(PaymentTossOperationStatus.TOSS_SUCCEEDED), any(Pageable.class)))
                .willReturn(List.of(operation));

        // when
        recoveryService.recoverTossSucceededOperations();

        // then
        verify(paymentCancellationRecorder).recordTossPaymentCanceled(7L, "고객 요청");
        verify(operationService).markLocalRecorded("payment-cancel-7");
        verify(paymentCompletionRecorder, never()).recordTossPaymentCompleted(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyList()
        );
    }

    @Test
    @DisplayName("복구 실패 기록까지 실패해도 다음 operation 복구를 계속한다")
    void recoverTossSucceededOperations_WhenFailureMarkFails_ContinuesNextOperation() {
        // given
        PaymentTossOperationRecoveryService recoveryService = service();
        PaymentTossOperationEntity failedConfirmOperation = PaymentTossOperationEntity.confirm(
                "payment-confirm-7",
                "order-7",
                7L,
                45000,
                "tgen_7"
        );
        failedConfirmOperation.markConfirmTossSucceeded("카드", LocalDateTime.now());
        PaymentTossOperationEntity cancelOperation = PaymentTossOperationEntity.cancel(
                "payment-cancel-8",
                "order-8",
                8L,
                "tgen_8",
                "고객 요청"
        );
        cancelOperation.markCancelTossSucceeded();

        given(operationRepository.findByStatus(eq(PaymentTossOperationStatus.TOSS_SUCCEEDED), any(Pageable.class)))
                .willReturn(List.of(failedConfirmOperation, cancelOperation));
        given(orderSalesClient.getSales(7L)).willThrow(new RuntimeException("order 장애"));
        willThrow(new RuntimeException("operation 저장 장애"))
                .given(operationService)
                .markLocalRecordFailed(eq("payment-confirm-7"), any(Exception.class));

        // when
        recoveryService.recoverTossSucceededOperations();

        // then
        verify(operationService).markLocalRecordFailed(eq("payment-confirm-7"), any(Exception.class));
        verify(paymentCancellationRecorder).recordTossPaymentCanceled(8L, "고객 요청");
        verify(operationService).markLocalRecorded("payment-cancel-8");
    }

    private PaymentTossOperationRecoveryService service() {
        return new PaymentTossOperationRecoveryService(
                operationRepository,
                paymentCompletionRecorder,
                paymentCancellationRecorder,
                operationService,
                orderSalesClient
        );
    }

    private OrderSalesResponse.SalesDetail testSalesDetail() {
        return new OrderSalesResponse.SalesDetail(
                7L,
                "order-7",
                "PENDING",
                List.of(new OrderSalesResponse.SalesItemDetail(
                        1L,
                        1999L,
                        "상품_1999",
                        1,
                        45000,
                        45000
                )),
                1,
                45000
        );
    }
}
