package com.project.chaechaeserver.application.response.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResGetInventoryDTO {

    private Inventory inventory;


    public static ResGetInventoryDTO from(InventoryEntity inventoryEntity) {
        return ResGetInventoryDTO.builder()
            .inventory(Inventory.from(inventoryEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inventory {
        private String productName;
        private Integer quantity;
        private Long productId;
        private Long inventoryId;


        public static Inventory from(InventoryEntity inventoryEntity) {
            return Inventory.builder()
                .productName(inventoryEntity.getProduct().getName())
                .quantity(inventoryEntity.getQuantity())
                .productId(inventoryEntity.getProduct().getId())
                .inventoryId(inventoryEntity.getId())
                .build();
        }
    }
}
