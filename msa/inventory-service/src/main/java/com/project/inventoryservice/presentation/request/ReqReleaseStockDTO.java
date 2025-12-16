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
public class ReqReleaseStockDTO {

    @NotBlank(message = "orderId는 필수입니다")
    private String orderId;

    @NotNull(message = "salesId는 필수입니다")
    private Long salesId;

    @NotEmpty(message = "해제할 상품 목록은 필수입니다")
    @Valid
    private List<ReleaseItem> items;

    private String reason;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseItem {
        @NotNull(message = "productId는 필수입니다")
        private Long productId;
    }
}
