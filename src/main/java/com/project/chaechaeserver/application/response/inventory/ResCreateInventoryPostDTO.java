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

    private InventoryInfo inventoryInfo;

    public static ResCreateInventoryPostDTO from(InventoryEntity inventoryEntity) {
        return ResCreateInventoryPostDTO.builder()
            .inventoryInfo(InventoryInfo.from(inventoryEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryInfo {
        private Long inventoryId;
        private Long productId;
        private Integer quantity;


        public static InventoryInfo from(InventoryEntity inventoryEntity) {
            return InventoryInfo.builder()
                .inventoryId(inventoryEntity.getId())
                .productId(inventoryEntity.getProducts().getId())
                .quantity(inventoryEntity.getQuantity())
                .build();
        }

    }
}
