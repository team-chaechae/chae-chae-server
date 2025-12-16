package com.project.inventoryservice.application.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResReleaseStockDTO {

    private String orderId;
    private Long salesId;
    private boolean success;
    private String message;

    public static ResReleaseStockDTO success(String orderId, Long salesId) {
        return ResReleaseStockDTO.builder()
                .orderId(orderId)
                .salesId(salesId)
                .success(true)
                .message("재고 예약 해제 완료")
                .build();
    }

    public static ResReleaseStockDTO failed(String orderId, Long salesId, String reason) {
        return ResReleaseStockDTO.builder()
                .orderId(orderId)
                .salesId(salesId)
                .success(false)
                .message(reason)
                .build();
    }
}
