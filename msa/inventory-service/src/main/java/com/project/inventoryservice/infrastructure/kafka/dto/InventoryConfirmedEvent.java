package com.project.inventoryservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 재고 차감 성공 이벤트
 * inventory-confirmed 토픽으로 발행
 * order-service에서 수신하여 SSE로 클라이언트에 알림
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryConfirmedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private LocalDateTime confirmedAt;

    public static InventoryConfirmedEvent of(String orderId, Long salesId) {
        return InventoryConfirmedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .confirmedAt(LocalDateTime.now())
                .build();
    }
}
