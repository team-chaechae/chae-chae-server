package com.project.inventoryservice.application.response.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 판매 재고 변경 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResSalesInventoryDTO {

    private boolean success;
    private int processedCount;
    private List<InventoryChangeResult> results;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryChangeResult {
        private Long productId;
        private Integer changedQuantity;
        private Integer currentStock;
    }

    public static ResSalesInventoryDTO success(List<InventoryChangeResult> results) {
        return ResSalesInventoryDTO.builder()
                .success(true)
                .processedCount(results.size())
                .results(results)
                .build();
    }
}
