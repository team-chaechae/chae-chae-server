package com.project.inventoryservice.infrastructure.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 재고 변경 이벤트
 * Kafka로 발행되어 비동기로 inventory 히스토리 저장
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {

    /**
     * 이벤트 ID (멱등성 보장용)
     */
    private String eventId;

    /**
     * 상품 ID
     */
    private Long productId;

    /**
     * 변경량 (양수: 증가, 음수: 감소)
     */
    private Integer quantity;

    /**
     * 변경 타입 (RECEIVE, ORDER_DECREASE, ORDER_RESTORE, ADJUST)
     */
    private String changeType;

    /**
     * 이벤트 발생 시각
     */
    private LocalDateTime occurredAt;

    /**
     * 변경 후 재고 (Redis에서 가져온 값)
     */
    private Integer currentStock;
}
