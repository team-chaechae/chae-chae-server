package com.project.inventoryservice.application.response.warehouse;

import com.project.inventoryservice.domain.model.InventoryEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 물류 출고 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResWarehouseShipmentDTO {

    private List<ShipmentResult> inventory;

    public static ResWarehouseShipmentDTO from(List<InventoryEntity> inventoryEntity) {
        return ResWarehouseShipmentDTO.builder()
            .inventory(ShipmentResult.from(inventoryEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentResult {
        private Long inventoryId;
        private Long productId;
        private Integer quantity;

        public static List<ShipmentResult> from(List<InventoryEntity> inventoryEntityList) {
            return inventoryEntityList.stream()
                .map(ShipmentResult::from)
                .toList();
        }

        public static ShipmentResult from(InventoryEntity inventoryEntity) {
            return ShipmentResult.builder()
                .inventoryId(inventoryEntity.getId())
                .productId(inventoryEntity.getProductId())
                .quantity(inventoryEntity.getQuantity())
                .build();
        }
    }
}
