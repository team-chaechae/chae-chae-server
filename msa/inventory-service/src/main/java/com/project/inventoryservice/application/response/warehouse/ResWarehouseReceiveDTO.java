package com.project.inventoryservice.application.response.warehouse;

import com.project.inventoryservice.domain.model.InventoryEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 물류 입고 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResWarehouseReceiveDTO {

    private List<ReceiveResult> inventory;

    public static ResWarehouseReceiveDTO from(List<InventoryEntity> inventoryEntity) {
        return ResWarehouseReceiveDTO.builder()
            .inventory(ReceiveResult.from(inventoryEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveResult {
        private Long inventoryId;
        private Long productId;
        private Integer quantity;

        public static List<ReceiveResult> from(List<InventoryEntity> inventoryEntityList) {
            return inventoryEntityList.stream()
                .map(ReceiveResult::from)
                .toList();
        }

        public static ReceiveResult from(InventoryEntity inventoryEntity) {
            return ReceiveResult.builder()
                .inventoryId(inventoryEntity.getId())
                .productId(inventoryEntity.getProductId())
                .quantity(inventoryEntity.getQuantity())
                .build();
        }
    }
}
