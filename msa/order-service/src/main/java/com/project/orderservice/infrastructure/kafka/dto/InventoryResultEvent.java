package com.project.orderservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 재고 처리 결과 이벤트 (Inventory → Order)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResultEvent {

    private Long salesId;
    private ResultStatus status;
    private String failureReason;
    private List<ItemResult> results;

    public enum ResultStatus {
        SUCCESS,
        FAILED
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResult {
        private Long productId;
        private Integer currentStock;
    }

    public boolean isSuccess() {
        return status == ResultStatus.SUCCESS;
    }
}
