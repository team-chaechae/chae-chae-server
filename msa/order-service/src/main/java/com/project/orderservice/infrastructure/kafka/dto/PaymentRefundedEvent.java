package com.project.orderservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 결제 환불 이벤트 (payment-service에서 발행)
 * 재고 차감 실패 시 환불 처리 후 발행
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private String reason;
    private LocalDateTime refundedAt;
}
