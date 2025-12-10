package com.project.inventoryservice.application.response.warehouse;

import com.project.inventoryservice.domain.model.InventoryEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 물류 재고 조정 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResWarehouseModifyDTO {

    private List<ModifyResult> inventory;

    public static ResWarehouseModifyDTO from(List<InventoryEntity> inventoryEntity) {
        return ResWarehouseModifyDTO.builder()
            .inventory(ModifyResult.from(inventoryEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModifyResult {
        private Long inventoryId;
        private Long productId;
        private Integer quantity;

        public static List<ModifyResult> from(List<InventoryEntity> inventoryEntityList) {
            return inventoryEntityList.stream()
                .map(ModifyResult::from)
                .toList();
        }

        public static ModifyResult from(InventoryEntity inventoryEntity) {
            return ModifyResult.builder()
                .inventoryId(inventoryEntity.getId())
                .productId(inventoryEntity.getProductId())
                .quantity(inventoryEntity.getQuantity())
                .build();
        }
    }
}
