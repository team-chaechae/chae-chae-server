package com.project.inventoryservice.presentation.request.warehouse;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 물류 입고 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqWarehouseReceiveDTO {

    private List<ReceiveItem> inventory;

    public List<Long> getProductIds() {
        return inventory.stream()
            .map(ReceiveItem::getProductId)
            .toList();
    }

    public List<Integer> getQuantities() {
        return inventory.stream()
            .map(ReceiveItem::getQuantity)
            .toList();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveItem {
        private Long productId;
        private Integer quantity;
    }
}
