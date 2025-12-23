package com.project.paymentservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 차감 실패 이벤트 (inventory-service에서 발행)
 * 결제 환불 요청용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedEvent {

    private String orderId;
    private Long salesId;
    private String reason;
    private LocalDateTime failedAt;
}
