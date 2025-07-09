package com.project.chaechaeserver.application.response.inventory;


import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResCreateInventoryPostDTO {

    private Inventory inventory;

    public static ResCreateInventoryPostDTO from(InventoryEntity inventoryEntity) {
        return ResCreateInventoryPostDTO.builder()
            .inventory(Inventory.from(inventoryEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inventory {
        private Long inventoryId;
        private Long productId;
        private Integer quantity;


        public static Inventory from(InventoryEntity inventoryEntity) {
            return Inventory.builder()
                .inventoryId(inventoryEntity.getId())
                .productId(inventoryEntity.getProduct().getId())
                .quantity(inventoryEntity.getQuantity())
                .build();
        }

    }
}
