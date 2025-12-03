package com.project.inventoryservice.presentation.request.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 내부 서비스용 재고 변경 요청 DTO
 * order-service 등에서 호출 시 사용
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqInventoryChangeDTO {

    private List<InventoryChangeItem> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryChangeItem {
        private Long productId;
        private Integer quantity;
    }
}
