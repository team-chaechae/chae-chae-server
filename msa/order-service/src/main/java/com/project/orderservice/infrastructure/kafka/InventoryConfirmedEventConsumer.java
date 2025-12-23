package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import com.project.orderservice.infrastructure.sse.NotificationEvent;
import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 재고 차감 성공 이벤트 Consumer
 *
 * inventory-confirmed 이벤트 수신 → SSE로 클라이언트에 주문 확정 알림
 * 클라이언트의 "결제중입니다..." 메시지를 해제하는 역할
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

        try {
            log.info("[재고 차감 성공 이벤트 수신] orderId: {}, salesId: {}", orderId, salesId);

            // SSE로 클라이언트에게 주문 확정 알림 전송
            NotificationEvent notification = NotificationEvent.paymentConfirmed(orderId, salesId);
            sseEmitterRegistry.sendEvent(orderId, notification);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("[SSE 알림 전송 실패] orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getMessage());
            ack.acknowledge();  // SSE 실패는 비즈니스 영향 없으므로 ack 처리
        }
    }
}
