package com.project.chaechaeserver.presentation.request.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqOrderCustomerPostCreateDTO {

    @Valid
    @NotNull(message = "주문정보를 입력해주세요")
    private Order order;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Order {

        @NotNull(message = "고객 ID를 입력해주세요")
        private Long customerId;

        @Valid
        @NotNull(message = "주문 상품을 입력해주세요")
        @Size(min = 1, message = "최소 1개 이상의 상품이 필요합니다")
        private List<OrderItem> orderItems;

        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class OrderItem {

            @NotNull(message = "상품 ID를 입력해주세요")
            private Integer productId;

            @NotNull(message = "상품 이름을 입력해주세요")
            private String productName;

            @NotNull(message = "수량을 입력해주세요")
            @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
            private Integer quantity;

            @NotNull(message = "가격을 입력해주세요")
            @Min(value = 0, message = "가격은 0원 이상이어야 합니다")
            private Integer price;


        }
    }
}