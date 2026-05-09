package com.project.orderservice.infrastructure.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSE 알림 이벤트 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String eventType;      // INVENTORY_FAILED, PAYMENT_CONFIRMED 등
    private String orderId;
    private Long salesId;
    private String message;
    private LocalDateTime occurredAt;

    public static NotificationEvent inventoryFailed(String orderId, Long salesId, String reason) {
        return NotificationEvent.builder()
                .eventType("INVENTORY_FAILED")
                .orderId(orderId)
                .salesId(salesId)
                .message(reason)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    public static NotificationEvent paymentConfirmed(String orderId, Long salesId) {
        return NotificationEvent.builder()
                .eventType("PAYMENT_CONFIRMED")
                .orderId(orderId)
                .salesId(salesId)
                .message("결제가 완료되었습니다.")
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
