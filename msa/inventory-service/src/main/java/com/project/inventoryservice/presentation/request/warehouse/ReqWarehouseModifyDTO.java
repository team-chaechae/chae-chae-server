package com.project.inventoryservice.presentation.request.warehouse;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 물류 재고 조정/출고 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqWarehouseModifyDTO {

    private List<ModifyItem> inventory;

    public List<Long> getProductIds() {
        return inventory.stream()
            .map(ModifyItem::getProductId)
            .toList();
    }

    public List<Integer> getQuantities() {
        return inventory.stream()
            .map(ModifyItem::getQuantity)
            .toList();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifyItem {
        private Long productId;
        private Integer quantity;
    }
}
