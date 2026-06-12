package com.project.orderservice.presentation.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateSalesDTO {

    @NotNull(message = "사용자 ID는 필수입니다.")
    @Min(value = 1, message = "사용자 ID는 1 이상이어야 합니다.")
    private Long userId;

    @NotNull(message = "배송지 정보는 필수입니다.")
    @Valid
    private DeliveryAddress deliveryAddress;

    @NotEmpty(message = "판매 항목은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<SalesItem> salesItems;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeliveryAddress {

        @NotBlank(message = "수령인 이름은 필수입니다.")
        @Size(max = 80, message = "수령인 이름은 80자 이하여야 합니다.")
        private String recipientName;

        @NotBlank(message = "수령인 연락처는 필수입니다.")
        @Size(max = 30, message = "수령인 연락처는 30자 이하여야 합니다.")
        private String recipientPhone;

        @NotBlank(message = "우편번호는 필수입니다.")
        @Size(max = 20, message = "우편번호는 20자 이하여야 합니다.")
        private String zipCode;

        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
        private String address;

        @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
        private String addressDetail;

        @Size(max = 255, message = "배송 메모는 255자 이하여야 합니다.")
        private String deliveryMemo;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesItem {

        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private Integer quantity;
    }
}
