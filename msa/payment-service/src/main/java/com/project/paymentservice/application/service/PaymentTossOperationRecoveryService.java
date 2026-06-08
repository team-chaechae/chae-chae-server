package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.domain.model.PaymentTossOperationEntity;
import com.project.paymentservice.domain.model.PaymentTossOperationStatus;
import com.project.paymentservice.domain.model.PaymentTossOperationType;
import com.project.paymentservice.domain.repository.PaymentTossOperationRepository;
import com.project.paymentservice.infrastructure.order.OrderSalesClient;
import com.project.paymentservice.infrastructure.order.dto.OrderSalesResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTossOperationRecoveryService {

    private final PaymentTossOperationRepository operationRepository;
    private final PaymentCompletionRecorder paymentCompletionRecorder;
    private final PaymentCancellationRecorder paymentCancellationRecorder;
    private final PaymentTossOperationService operationService;
    private final OrderSalesClient orderSalesClient;

    @Scheduled(fixedDelayString = "${payment.toss-operation.recovery-interval-ms:60000}")
    public void recoverTossSucceededOperations() {
        List<PaymentTossOperationEntity> operations = operationRepository.findByStatus(
                PaymentTossOperationStatus.TOSS_SUCCEEDED,
                PageRequest.of(0, 50)
        );

        for (PaymentTossOperationEntity operation : operations) {
            recover(operation);
        }
    }

    private void recover(PaymentTossOperationEntity operation) {
        try {
            if (operation.getOperationType() == PaymentTossOperationType.CONFIRM) {
                recoverConfirm(operation);
            } else {
                recoverCancel(operation);
            }
            operationService.markLocalRecorded(operation.getOperationId());
        } catch (Exception e) {
            log.warn("[Payment Toss Operation 복구 실패] operationId: {}, salesId: {}, error: {}",
                    operation.getOperationId(), operation.getSalesId(), e.getMessage());
            markRecoveryFailed(operation, e);
        }
    }

    private void markRecoveryFailed(PaymentTossOperationEntity operation, Exception cause) {
        try {
            operationService.markLocalRecordFailed(operation.getOperationId(), cause);
        } catch (Exception e) {
            log.error("[Payment Toss Operation 복구 실패 기록 실패] operationId: {}, salesId: {}, error: {}",
                    operation.getOperationId(), operation.getSalesId(), e.getMessage());
        }
    }

    private void recoverConfirm(PaymentTossOperationEntity operation) {
        OrderSalesResponse.SalesDetail sales = orderSalesClient.getSales(operation.getSalesId());
        paymentCompletionRecorder.recordTossPaymentCompleted(
                operation.getOrderId(),
                operation.getSalesId(),
                operation.getAmount(),
                operation.getTossPaymentKey(),
                operation.getPaymentMethod(),
                operation.getApprovedAt(),
                toPaymentCompletedItems(sales.items())
        );
    }

    private void recoverCancel(PaymentTossOperationEntity operation) {
        paymentCancellationRecorder.recordTossPaymentCanceled(
                operation.getSalesId(),
                operation.getReason()
        );
    }

    private List<PaymentCompletedInternalEvent.OrderItem> toPaymentCompletedItems(
            List<OrderSalesResponse.SalesItemDetail> items
    ) {
        return items.stream()
                .map(item -> PaymentCompletedInternalEvent.OrderItem.builder()
                        .productId(item.productId())
                        .productName(item.productName())
                        .quantity(item.quantity())
                        .price(item.price())
                        .build())
                .toList();
    }
}
