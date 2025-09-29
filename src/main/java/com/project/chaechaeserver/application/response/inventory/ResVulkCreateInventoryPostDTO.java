package com.project.chaechaeserver.application.response.inventory;


import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResVulkCreateInventoryPostDTO {

    private List<Inventory> inventory;

    public static ResVulkCreateInventoryPostDTO from(List<InventoryEntity> inventoryEntity) {
        return ResVulkCreateInventoryPostDTO.builder()
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

        public static List<Inventory> from(List<InventoryEntity> inventoryEntityList) {
            return inventoryEntityList.stream()
                .map(Inventory::from)
                .toList();
        }

        public static Inventory from(InventoryEntity inventoryEntity) {
            return Inventory.builder()
                .inventoryId(inventoryEntity.getId())
                .productId(inventoryEntity.getProduct().getId())
                .quantity(inventoryEntity.getQuantity())
                .build();
        }

    }
}