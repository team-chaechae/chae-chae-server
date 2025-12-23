package com.project.inventoryservice.application.response.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 내부 서비스용 재고 변경 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResInventoryChangeDTO {

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

    public static ResInventoryChangeDTO success(List<InventoryChangeResult> results) {
        return ResInventoryChangeDTO.builder()
                .success(true)
                .processedCount(results.size())
                .results(results)
                .build();
    }
}
