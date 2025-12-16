package com.project.inventoryservice.application.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResConfirmStockDTO {

    private String orderId;
    private Long salesId;
    private boolean success;
    private String message;

    public static ResConfirmStockDTO success(String orderId, Long salesId) {
        return ResConfirmStockDTO.builder()
                .orderId(orderId)
                .salesId(salesId)
                .success(true)
                .message("재고 확정 완료")
                .build();
    }

    public static ResConfirmStockDTO failed(String orderId, Long salesId, String reason) {
        return ResConfirmStockDTO.builder()
                .orderId(orderId)
                .salesId(salesId)
                .success(false)
                .message(reason)
                .build();
    }
}
