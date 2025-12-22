package com.project.orderservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 차감 성공 이벤트 (inventory-service에서 발행)
 * 재고 차감 완료 후 발행 → SSE로 클라이언트에 주문 확정 알림
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
}
