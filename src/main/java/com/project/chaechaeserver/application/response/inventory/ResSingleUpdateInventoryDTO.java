package com.project.chaechaeserver.application.response.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResSingleUpdateInventoryDTO {

    private InventoryHistory history;
    private CurrentStock currentStock;

    public static ResSingleUpdateInventoryDTO from(ProductEntity productEntity) {
        InventoryEntity latestInventory = productEntity.getInventoryHistories()
            .get(productEntity.getInventoryHistories().size() - 1);

        return ResSingleUpdateInventoryDTO.builder()
            .history(InventoryHistory.from(latestInventory))
            .currentStock(CurrentStock.from(productEntity))
            .build();
    }

    public static ResSingleUpdateInventoryDTO from(ProductEntity productEntity, InventoryEntity latestHistory) {
        return ResSingleUpdateInventoryDTO.builder()
            .history(InventoryHistory.from(latestHistory))
            .currentStock(CurrentStock.from(productEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryHistory {
        private Long historyId;
        private Long productId;
        private Integer changeAmount;

        public static InventoryHistory from(InventoryEntity inventoryEntity) {
            return InventoryHistory.builder()
                .historyId(inventoryEntity.getId())
                .productId(inventoryEntity.getProduct().getId())
                .changeAmount(inventoryEntity.getQuantity())
                .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentStock {
        private Long productId;
        private Integer currentQuantity;

        public static CurrentStock from(ProductEntity productEntity) {
            return CurrentStock.builder()
                .productId(productEntity.getId())
                .currentQuantity(productEntity.getQuantity())
                .build();
        }
    }
}