package com.project.paymentservice.application.event;

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
public class PaymentCompletedInternalEvent {

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

    public static PaymentCompletedInternalEvent of(String orderId, Long salesId, Integer totalAmount, List<OrderItem> items) {
        return PaymentCompletedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .totalAmount(totalAmount)
                .items(items)
                .completedAt(LocalDateTime.now())
                .build();
    }

    public String getMessageKey() {
        return orderId;
    }

    public String getAggregateId() {
        return String.valueOf(salesId);
    }
}
