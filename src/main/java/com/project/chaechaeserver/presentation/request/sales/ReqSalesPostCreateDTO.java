package com.project.chaechaeserver.presentation.request.sales;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqSalesPostCreateDTO {

    @Valid
    @NotNull(message = "판매정보를 입력해주세요")
    private Sales sales;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sales {

        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 1 이상의 양수여야 합니다.")
        private Long productId;

        @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
        private int quantity;

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        private int price;

    }
}
