package com.project.orderservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * inventory-service 호출용 재고 변경 요청/응답 DTO
 */
public class InventoryChangeDTO {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String operationId;
        private List<InventoryChangeItem> items;

        public static Request of(Long productId, Integer quantity) {
            return Request.builder()
                    .items(List.of(InventoryChangeItem.builder()
                            .productId(productId)
                            .quantity(quantity)
                            .build()))
                    .build();
        }

        public static Request of(String operationId, Long productId, Integer quantity) {
            return Request.builder()
                    .operationId(operationId)
                    .items(List.of(InventoryChangeItem.builder()
                            .productId(productId)
                            .quantity(quantity)
                            .build()))
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryChangeItem {
        private Long productId;
        private Integer quantity;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private boolean success;
        private boolean duplicate;
        private int processedCount;
        private List<InventoryChangeResult> results;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryChangeResult {
        private Long productId;
        private Integer changedQuantity;
        private Integer currentStock;
    }
}
