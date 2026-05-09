package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 재고 차감 성공 이벤트 Consumer
 *
 * inventory-confirmed 이벤트 수신 → SSE로 클라이언트에 주문 확정 알림
 *
 * 주의: SSE 알림은 비즈니스 로직이 아니므로 실패해도 DLQ로 보내지 않음.
 * 클라이언트가 연결이 끊어져 SSE 전송이 실패해도 주문은 정상 처리됨.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryConfirmedEventConsumer {

    private static final String TOPIC = "inventory-confirmed";

    private final SseEmitterRegistry sseEmitterRegistry;

    @KafkaListener(
            topics = TOPIC,
            groupId = "order-inventory-confirmed-group",
            containerFactory = "inventoryConfirmedListenerFactory",
            concurrency = "3"
    )
    public void handleInventoryConfirmed(InventoryConfirmedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        var previousMdc = MDC.getCopyOfContextMap();

        try {
            MDC.put("orderId", orderId);
            MDC.put("salesId", String.valueOf(salesId));
            log.info("[재고 차감 성공 이벤트 수신] orderId: {}, salesId: {}", orderId, salesId);

            // SSE로 클라이언트에게 주문 확정 알림 전송
            NotificationEvent notification = NotificationEvent.paymentConfirmed(orderId, salesId);
            sseEmitterRegistry.sendEvent(orderId, notification);

        } catch (Exception e) {
            // SSE 실패는 비즈니스 영향 없음 (클라이언트 연결 끊김 등)
            log.warn("[SSE 알림 전송 실패] orderId: {}, salesId: {}, error: {} - 비즈니스 영향 없음",
                    orderId, salesId, e.getMessage());
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
            // SSE 성공/실패 관계없이 ack (비즈니스 트랜잭션 아님)
            ack.acknowledge();
        }
    }
}
