package com.project.orderservice.application.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 내부 이벤트 (ApplicationEventPublisher용)
 *
 * 트랜잭션 내에서 발행되어:
 * - BEFORE_COMMIT: Outbox 테이블에 기록
 * - AFTER_COMMIT: Kafka로 메시지 발행
 */
@Getter
@Builder
public class OrderCreatedInternalEvent {

    private final String orderId;
    private final Long salesId;
    private final List<OrderItem> items;
    private final Integer totalAmount;
    private final LocalDateTime createdAt;

    @Getter
    @Builder
    public static class OrderItem {
        private final Long productId;
        private final String productName;
        private final Integer quantity;
        private final Integer price;
    }

    /**
     * Outbox aggregateId (salesId 기준)
     */
    public String getAggregateId() {
        return String.valueOf(salesId);
    }

    /**
     * Kafka 메시지 키 (orderId 기준 - 파티셔닝)
     */
    public String getMessageKey() {
        return orderId;
    }

    public static OrderCreatedInternalEvent of(String orderId, Long salesId,
            List<OrderItem> items, Integer totalAmount) {
        return OrderCreatedInternalEvent.builder()
                .orderId(orderId)
                .salesId(salesId)
                .items(items)
                .totalAmount(totalAmount)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
