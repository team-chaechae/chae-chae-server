package com.project.inventoryservice.presentation.request.bulk;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqUpdateInventoryDTO {


    private List<Inventory> inventory;

    public List<Long> getProductIds() {
        return inventory.stream()
            .map(Inventory::getProductId)
            .toList();
    }

    public List<Integer> getQuantities() {
        return inventory.stream()
            .map(Inventory::getQuantity)
            .toList();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inventory {

        private Long productId;
        private Integer quantity;

    }
}
