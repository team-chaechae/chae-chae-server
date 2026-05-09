package com.project.inventoryservice.presentation.request.sales;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 판매 재고 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqSalesInventoryDTO {

    private List<SalesInventoryItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesInventoryItem {
        private Long productId;
        private Integer quantity;
    }
}
