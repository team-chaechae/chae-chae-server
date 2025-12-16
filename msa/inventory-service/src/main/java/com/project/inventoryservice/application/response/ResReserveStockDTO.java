package com.project.inventoryservice.application.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResReserveStockDTO {

    private String orderId;
    private Long salesId;
    private boolean success;
    private String failureReason;
    private List<ItemResult> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private Long productId;
        private Integer requestedQuantity;
        private Integer reservedQuantity;
        private Integer availableStock;
        private boolean success;
        private String errorReason;
    }

    public static ResReserveStockDTO success(String orderId, Long salesId, List<ItemResult> items) {
        return ResReserveStockDTO.builder()
                .orderId(orderId)
                .salesId(salesId)
                .success(true)
                .items(items)
                .build();
    }

    public static ResReserveStockDTO failed(String orderId, Long salesId, List<ItemResult> items, String reason) {
        return ResReserveStockDTO.builder()
                .orderId(orderId)
                .salesId(salesId)
                .success(false)
                .failureReason(reason)
                .items(items)
                .build();
    }
}
