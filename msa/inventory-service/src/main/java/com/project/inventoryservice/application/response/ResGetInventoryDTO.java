package com.project.inventoryservice.application.response;

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



    public static ResGetInventoryDTO fromDto(InventoryWithProductDto dto) {
        return ResGetInventoryDTO.builder()
            .inventory(Inventory.fromDto(dto))
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



        public static Inventory fromDto(InventoryWithProductDto dto) {
            return Inventory.builder()
                .productName(dto.getProductName())
                .quantity(dto.getQuantity())
                .productId(dto.getProductId())
                .inventoryId(dto.getInventoryId())
                .build();
        }
    }
}
