package com.project.paymentservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

    private String eventId;
    private String orderId;
    private Long salesId;
    private Integer totalAmount;
    private List<OrderItem> items;
    private LocalDateTime completedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private Long productId;
        private String productName;
        private Integer quantity;
        private Integer price;
    }

    public static PaymentCompletedEvent of(String orderId, Long salesId, Integer totalAmount, List<OrderItem> items) {
        return PaymentCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .totalAmount(totalAmount)
                .items(items)
                .completedAt(LocalDateTime.now())
                .build();
    }
}
