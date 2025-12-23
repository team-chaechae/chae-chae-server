package com.project.orderservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 재고 차감 요청 이벤트 (Order → Inventory)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReserveEvent {

    private Long salesId;
    private List<Item> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long productId;
        private Integer quantity;
    }
}
