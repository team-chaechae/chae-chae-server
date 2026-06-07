package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.infrastructure.client.InventoryFeignClient;
import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

/**
 * 결제 환불 이벤트 Consumer
 *
 * payment-refunded 이벤트 수신 → 주문 상태를 CANCELLED로 변경
 * (재고 차감 실패로 인한 환불 처리)
 * 비즈니스 로직은 SalesService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundedEventConsumer {

    private static final String TOPIC = "payment-refunded";

    private final SalesService salesService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SlackAlertService slackAlertService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final InventoryFeignClient inventoryFeignClient;

    @KafkaListener(
            topics = TOPIC,
            groupId = "order-payment-refund-group",
            containerFactory = "paymentRefundedListenerFactory",
            concurrency = "3"
    )
    public void handlePaymentRefunded(PaymentRefundedEvent event, Acknowledgment ack) {
        Long salesId = event.getSalesId();
        String orderId = event.getOrderId();
        String reason = event.getReason() == null ? "결제 환불" : event.getReason();

        try {
            kafkaBackpressureExecutor.execute(() -> {
                try {
                    log.info("[결제 환불 이벤트 수신] orderId: {}, salesId: {}", orderId, salesId);

                    ResSalesGetByIdDTO.SalesDetail salesBeforeCancel = null;
                    if (shouldRestoreInventory(event)) {
                        salesBeforeCancel = getSalesBeforeCancel(salesId, orderId);
                    }

                    salesService.cancelSales(salesId, orderId, reason);
                    if (shouldRestoreInventory(event) && isCompletedSales(salesBeforeCancel)) {
                        restoreInventory(salesId, orderId, salesBeforeCancel);
                    }

                    sendNotification(orderId, salesId, event);

                    ack.acknowledge();
                } catch (Exception e) {
                    log.error("[주문 취소 실패] orderId: {}, salesId: {}, error: {}",
                            orderId, salesId, e.getMessage());
                    slackAlertService.sendKafkaErrorAlert(TOPIC,
                            "주문 취소 실패 - orderId: " + orderId + ", salesId: " + salesId, e);
                    // ack하지 않아 offset commit을 막고 재처리 가능 상태로 둔다.
                    // 주문 취소와 재고 복구는 상태 확인으로 중복 이벤트를 방어한다.
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, error: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }

    private boolean shouldRestoreInventory(PaymentRefundedEvent event) {
        String reason = event.getReason();
        return reason != null && !reason.contains("재고 차감 실패");
    }

    private ResSalesGetByIdDTO.SalesDetail getSalesBeforeCancel(Long salesId, String orderId) {
        ResSalesGetByIdDTO sales = salesService.getSalesBySalesId(salesId);
        if (sales.getSales() == null) {
            log.warn("[환불 재고 복구 스킵] 판매 정보 없음 - orderId: {}, salesId: {}", orderId, salesId);
            return null;
        }
        return sales.getSales();
    }

    private boolean isCompletedSales(ResSalesGetByIdDTO.SalesDetail sales) {
        return sales != null && "COMPLETED".equals(sales.getStatus());
    }

    private void restoreInventory(Long salesId, String orderId, ResSalesGetByIdDTO.SalesDetail sales) {
        if (sales.getItems() == null || sales.getItems().isEmpty()) {
            log.warn("[환불 재고 복구 스킵] 상품 정보 없음 - orderId: {}, salesId: {}", orderId, salesId);
            return;
        }

        List<InventoryChangeDTO.InventoryChangeItem> items = sales.getItems().stream()
                .map(item -> InventoryChangeDTO.InventoryChangeItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        InventoryChangeDTO.Response response = inventoryFeignClient.increaseInventory(
                InventoryChangeDTO.Request.builder()
                        .items(items)
                        .build()
        );
        log.info("[환불 재고 복구 완료] orderId: {}, salesId: {}, success: {}",
                orderId, salesId, response.isSuccess());
    }

    private NotificationEvent buildNotification(String orderId, Long salesId, PaymentRefundedEvent event) {
        if (shouldRestoreInventory(event)) {
            return NotificationEvent.inventoryFailed(orderId, salesId, "결제가 취소되어 주문이 취소되었습니다.");
        }
        return NotificationEvent.inventoryFailed(orderId, salesId, "재고 부족으로 인해 자동 환불 처리되었습니다.");
    }

    private void sendNotification(String orderId, Long salesId, PaymentRefundedEvent event) {
        try {
            NotificationEvent notification = buildNotification(orderId, salesId, event);
            sseEmitterRegistry.sendEvent(orderId, notification);
        } catch (Exception e) {
            log.warn("[SSE 알림 전송 실패] orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getMessage());
        }
    }
}
