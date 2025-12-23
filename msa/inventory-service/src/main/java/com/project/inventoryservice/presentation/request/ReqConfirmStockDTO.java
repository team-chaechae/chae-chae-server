package com.project.inventoryservice.presentation.request;

import jakarta.validation.Valid;
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
public class ReqConfirmStockDTO {

    @NotBlank(message = "orderId는 필수입니다")
    private String orderId;

    @NotNull(message = "salesId는 필수입니다")
    private Long salesId;

    @NotEmpty(message = "확정할 상품 목록은 필수입니다")
    @Valid
    private List<ConfirmItem> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmItem {
        @NotNull(message = "productId는 필수입니다")
        private Long productId;
    }
}
