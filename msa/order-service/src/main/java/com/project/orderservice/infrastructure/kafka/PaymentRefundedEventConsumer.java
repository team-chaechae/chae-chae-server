package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
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

    @KafkaListener(
            topics = TOPIC,
            groupId = "order-payment-refund-group",
            containerFactory = "paymentRefundedListenerFactory",
            concurrency = "3"
    )
    public void handlePaymentRefunded(PaymentRefundedEvent event, Acknowledgment ack) {
        Long salesId = event.getSalesId();
        String orderId = event.getOrderId();

        try {
            kafkaBackpressureExecutor.execute(() -> {
                try {
                    log.info("[결제 환불 이벤트 수신] orderId: {}, salesId: {}", orderId, salesId);

                    salesService.cancelSales(salesId, orderId, "재고 차감 실패로 인한 환불");

                    // SSE로 클라이언트에게 재고 부족 알림 전송
                    NotificationEvent notification = NotificationEvent.inventoryFailed(
                            orderId, salesId, "재고 부족으로 인해 자동 환불 처리되었습니다.");
                    sseEmitterRegistry.sendEvent(orderId, notification);

                    ack.acknowledge();
                } catch (Exception e) {
                    log.error("[주문 취소 실패] orderId: {}, salesId: {}, error: {}",
                            orderId, salesId, e.getMessage());
                    slackAlertService.sendKafkaErrorAlert(TOPIC,
                            "주문 취소 실패 - orderId: " + orderId + ", salesId: " + salesId, e);
                    ack.acknowledge();  // 중복 처리 방지
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, error: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }
}
