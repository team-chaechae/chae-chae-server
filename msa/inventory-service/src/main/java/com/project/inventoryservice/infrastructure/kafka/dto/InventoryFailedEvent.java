package com.project.inventoryservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 재고 차감 실패 이벤트
 * payment-service로 전달하여 환불 처리 요청
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private String reason;
    private LocalDateTime failedAt;

    public static InventoryFailedEvent of(String orderId, Long salesId, String reason) {
        return InventoryFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .reason(reason)
                .failedAt(LocalDateTime.now())
                .build();
    }
}
