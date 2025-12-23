package com.project.inventoryservice.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqReserveStockDTO {

    @NotBlank(message = "orderId는 필수입니다")
    private String orderId;

    @NotNull(message = "salesId는 필수입니다")
    private Long salesId;

    @NotEmpty(message = "예약할 상품 목록은 필수입니다")
    @Valid
    private List<ReserveItem> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReserveItem {
        @NotNull(message = "productId는 필수입니다")
        private Long productId;

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다")
        private Integer quantity;
    }
}
