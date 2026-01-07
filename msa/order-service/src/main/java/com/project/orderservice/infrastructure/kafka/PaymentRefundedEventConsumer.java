package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

/**
 * 결제 환불 이벤트 Consumer
 *
 * payment-refunded 이벤트 수신 → 주문 상태를 CANCELLED로 변경
 *
 * DLQ 패턴:
 * - 성공: ack
 * - 실패: 예외 throw → DlqErrorHandler가 DLQ로 발행
 * - 백프레셔 거부: ack 안함 → Kafka 재전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRefundedEventConsumer {

    private static final String TOPIC = "payment-refunded";

    private final SalesService salesService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final SlackAlertService slackAlertService;

    @KafkaListener(
            topics = "payment-refunded",
            groupId = "order-payment-refund-group",
            containerFactory = "paymentRefundedListenerFactory",
            concurrency = "3"
    )
    public void handlePaymentRefunded(PaymentRefundedEvent event, Acknowledgment ack) {
        Long salesId = event.getSalesId();
        String orderId = event.getOrderId();
        var previousMdc = MDC.getCopyOfContextMap();

        try {
            MDC.put("orderId", orderId);
            MDC.put("salesId", String.valueOf(salesId));
            var mdcContext = MDC.getCopyOfContextMap();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                } else {
                    MDC.clear();
                }
                try {
                log.info("[결제 환불 이벤트 수신] orderId: {}, salesId: {}", orderId, salesId);

                // 핵심 비즈니스 로직: 주문 취소
                salesService.cancelSales(salesId, orderId, "재고 차감 실패로 인한 환불");
                } finally {
                    MDC.clear();
                }
            }, kafkaBackpressureExecutor);

            future.get();
            ack.acknowledge();

            // SSE 알림은 비즈니스 로직 성공 후 별도 처리 (실패해도 ack 유지)
            sendSseNotification(orderId, salesId);

        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, error: {}", orderId, e.getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("백프레셔 작업 거부 - orderId: %s", orderId), e);
        } catch (ExecutionException e) {
            log.error("[주문 취소 실패 → DLQ] orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getCause().getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("주문 취소 실패 → DLQ - orderId: %s, salesId: %s", orderId, salesId),
                    (Exception) e.getCause());
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            slackAlertService.sendKafkaErrorAlert(TOPIC, "인터럽트 발생", e);
            throw new RuntimeException(e);
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    }

    /**
     * SSE 알림 전송 (비즈니스 핵심 아님, 실패해도 무시)
     */
    private void sendSseNotification(String orderId, Long salesId) {
        try {
            NotificationEvent notification = NotificationEvent.inventoryFailed(
                    orderId, salesId, "재고 부족으로 인해 자동 환불 처리되었습니다.");
            sseEmitterRegistry.sendEvent(orderId, notification);
        } catch (Exception e) {
            log.warn("[SSE 알림 전송 실패] orderId: {}, salesId: {}, error: {} - 비즈니스 영향 없음",
                    orderId, salesId, e.getMessage());
        }
    }
}
