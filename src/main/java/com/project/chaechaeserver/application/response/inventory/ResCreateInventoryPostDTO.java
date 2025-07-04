package com.project.chaechaeserver.application.response.inventory;


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

    public static ResCreateInventoryPostDTO of(Long id, Long productId, Integer quantity) {
        return ResCreateInventoryPostDTO.builder()
            .inventoryInfo(InventoryInfo.from(id, productId, quantity))
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


        public static InventoryInfo from(Long id, Long productId, Integer quantity) {
            return InventoryInfo.builder()
                .inventoryId(id)
                .productId(productId)
                .quantity(quantity)
                .build();
        }

    }
}
