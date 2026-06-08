package com.project.orderservice.application.service;

import com.project.orderservice.domain.model.PaymentOrchestrationEntity;
import com.project.orderservice.infrastructure.client.InventoryFeignClient;
import com.project.orderservice.infrastructure.client.PaymentFeignClient;
import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private static final String INVENTORY_DECREASE_FAILURE_REASON = "재고 차감 실패";
    private static final String ORDER_COMPLETE_FAILURE_REASON = "주문 완료 실패";
    private static final String DEFAULT_COMPENSATION_REASON = "결제 완료 오케스트레이션 보상";
    private static final String OPERATION_ID_PREFIX = "payment-orchestration:";

    private final SalesService salesService;
    private final InventoryFeignClient inventoryFeignClient;
    private final PaymentFeignClient paymentFeignClient;
    private final PaymentOrchestrationStateService orchestrationStateService;

    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        PaymentOrchestrationEntity orchestration = orchestrationStateService.getOrCreate(orderId, salesId);

        if (orchestration.isTerminal()) {
            log.info("[결제 완료 오케스트레이션 중복 이벤트 스킵] orderId: {}, salesId: {}, status: {}",
                    orderId, salesId, orchestration.getStatus());
            return;
        }

        if (orchestration.isCompensating()) {
            processCompensation(event, orchestration);
            return;
        }

        processNormalFlow(event, orchestration);
    }

    private void processNormalFlow(PaymentCompletedEvent event, PaymentOrchestrationEntity orchestration) {
        try {
            orchestration = deductInventoryIfNeeded(event, orchestration);
            orchestration = completeOrderIfNeeded(event, orchestration);

            log.info("[결제 완료 오케스트레이션 완료] orderId: {}, salesId: {}",
                    event.getOrderId(), event.getSalesId());
        } catch (OrchestrationStepException e) {
            startCompensation(event, orchestration, e.getReasonPrefix(), e.getCause());
        } catch (Exception e) {
            startCompensation(event, orchestration, resolveFailureReasonPrefix(orchestration), e);
        }
    }

    private PaymentOrchestrationEntity deductInventoryIfNeeded(
            PaymentCompletedEvent event,
            PaymentOrchestrationEntity orchestration
    ) {
        if (orchestration.isInventoryDeducted()) {
            return orchestration;
        }

        try {
            decreaseInventory(event);
            return orchestrationStateService.markInventoryDeducted(orchestration);
        } catch (Exception e) {
            throw new OrchestrationStepException(INVENTORY_DECREASE_FAILURE_REASON, e);
        }
    }

    private PaymentOrchestrationEntity completeOrderIfNeeded(
            PaymentCompletedEvent event,
            PaymentOrchestrationEntity orchestration
    ) {
        if (orchestration.isOrderCompleted()) {
            return orchestration;
        }

        try {
            salesService.completeSales(event.getSalesId(), event.getOrderId());
            return orchestrationStateService.markOrderCompleted(orchestration);
        } catch (Exception e) {
            throw new OrchestrationStepException(ORDER_COMPLETE_FAILURE_REASON, e);
        }
    }

    private void startCompensation(
            PaymentCompletedEvent event,
            PaymentOrchestrationEntity orchestration,
            String reasonPrefix,
            Throwable cause
    ) {
        String reason = buildReason(reasonPrefix, cause);
        PaymentOrchestrationEntity compensating =
                orchestrationStateService.startCompensation(orchestration, reason);
        processCompensation(event, compensating);
    }

    private void processCompensation(PaymentCompletedEvent event, PaymentOrchestrationEntity orchestration) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        String reason = resolveCompensationReason(orchestration);

        log.warn("[결제 완료 오케스트레이션 보상 시작] orderId: {}, salesId: {}, reason: {}",
                orderId, salesId, reason);

        orchestration = restoreInventoryIfNeeded(event, orchestration);
        orchestration = refundPaymentIfNeeded(orchestration, reason);
        cancelOrderIfNeeded(event, orchestration, reason);

        log.info("[결제 완료 오케스트레이션 보상 완료] orderId: {}, salesId: {}",
                orderId, salesId);
    }

    private PaymentOrchestrationEntity restoreInventoryIfNeeded(
            PaymentCompletedEvent event,
            PaymentOrchestrationEntity orchestration
    ) {
        if (!orchestration.isInventoryDeducted() || orchestration.isInventoryRestored()) {
            return orchestration;
        }

        restoreInventory(event);
        return orchestrationStateService.markInventoryRestored(orchestration);
    }

    private PaymentOrchestrationEntity refundPaymentIfNeeded(
            PaymentOrchestrationEntity orchestration,
            String reason
    ) {
        if (orchestration.isPaymentRefunded()) {
            return orchestration;
        }

        paymentFeignClient.refundPayment(orchestration.getSalesId(), reason);
        return orchestrationStateService.markPaymentRefunded(orchestration);
    }

    private void cancelOrderIfNeeded(
            PaymentCompletedEvent event,
            PaymentOrchestrationEntity orchestration,
            String reason
    ) {
        if (orchestration.isOrderCancelled()) {
            return;
        }

        salesService.cancelSales(event.getSalesId(), event.getOrderId(), reason);
        orchestrationStateService.markOrderCancelled(orchestration);
    }

    private void decreaseInventory(PaymentCompletedEvent event) {
        InventoryChangeDTO.Response response = inventoryFeignClient.decreaseInventory(
                InventoryChangeDTO.Request.builder()
                        .operationId(inventoryOperationId(event.getSalesId(), "inventory-deduct"))
                        .items(toInventoryItems(event.getItems()))
                        .build()
        );

        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("재고 차감 실패 응답");
        }
    }

    private void restoreInventory(PaymentCompletedEvent event) {
        InventoryChangeDTO.Response response = inventoryFeignClient.increaseInventory(
                InventoryChangeDTO.Request.builder()
                        .operationId(inventoryOperationId(event.getSalesId(), "inventory-restore"))
                        .items(toInventoryItems(event.getItems()))
                        .build()
        );

        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("재고 복구 실패 응답");
        }
    }

    private List<InventoryChangeDTO.InventoryChangeItem> toInventoryItems(
            List<PaymentCompletedEvent.OrderItem> items
    ) {
        validateInventoryItems(items);

        return items.stream()
                .map(item -> InventoryChangeDTO.InventoryChangeItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private void validateInventoryItems(List<PaymentCompletedEvent.OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("결제 완료 이벤트 상품 목록이 비어 있습니다.");
        }

        for (PaymentCompletedEvent.OrderItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException("결제 완료 이벤트 상품 항목이 비어 있습니다.");
            }

            if (item.getProductId() == null) {
                throw new IllegalArgumentException("결제 완료 이벤트 상품 ID가 비어 있습니다.");
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("결제 완료 이벤트 상품 수량은 1 이상이어야 합니다.");
            }
        }
    }

    private String buildReason(String reasonPrefix, Throwable cause) {
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return reasonPrefix;
        }
        return reasonPrefix + ": " + message;
    }

    private String resolveCompensationReason(PaymentOrchestrationEntity orchestration) {
        String lastError = orchestration.getLastError();
        if (lastError == null || lastError.isBlank()) {
            return DEFAULT_COMPENSATION_REASON;
        }
        return lastError;
    }

    private String resolveFailureReasonPrefix(PaymentOrchestrationEntity orchestration) {
        if (orchestration.isInventoryDeducted()) {
            return ORDER_COMPLETE_FAILURE_REASON;
        }
        return INVENTORY_DECREASE_FAILURE_REASON;
    }

    private String inventoryOperationId(Long salesId, String step) {
        return OPERATION_ID_PREFIX + salesId + ":" + step;
    }

    private static class OrchestrationStepException extends RuntimeException {

        private final String reasonPrefix;

        private OrchestrationStepException(String reasonPrefix, Throwable cause) {
            super(cause);
            this.reasonPrefix = reasonPrefix;
        }

        private String getReasonPrefix() {
            return reasonPrefix;
        }
    }
}
