package com.project.chaechaeserver.application.response.order;

import com.project.chaechaeserver.application.response.order.ResOrdersSearchDTO.OrdersPage.Orders.PageDetails;
import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.ProductInfo;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResOrdersSearchDTO {

    private OrdersPage ordersPage;

    public static ResOrdersSearchDTO from(Page<OrderEntity> orderEntityPage) {
        return ResOrdersSearchDTO.builder()
                .ordersPage(OrdersPage.from(orderEntityPage))
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrdersPage {

        private List<Orders> content;
        private PageDetails page;

        public static OrdersPage from(Page<OrderEntity> orderEntityPage) {
            return OrdersPage.builder()
                    .content(Orders.from(orderEntityPage.getContent()))
                    .page(PageDetails.from(orderEntityPage))
                    .build();
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Orders {

            @Schema(example = "1")
            private Long orderId;

            @Schema(example = "APPROVED")
            private StatusType status;

            @Schema(example = "1")
            private Long productId;

            @Schema(example = "감자 3kg")
            private String productName;

            @Schema(example = "야채")
            private String productCategory;

            @Schema(example = "6100")
            private Integer productPrice;

            @Schema(example = "10")
            private Integer quantity;

            @Schema(example = "61000")
            private Integer totalCost;

            @Schema(example = "john.doe@gamil.com")
            private String createdBy;
            private LocalDateTime createdAt;
            private LocalDateTime updatedAt;

            public static List<Orders> from(List<OrderEntity> orderEntityList) {
                return orderEntityList.stream()
                        .map(Orders::from)
                        .toList();
            }

            public static Orders from(OrderEntity orderEntity) {
                ProductInfo productInfo = orderEntity.getProductInfo();
                return Orders.builder()
                        .orderId(orderEntity.getId())
                        .status(orderEntity.getStatus())
                        .productId(productInfo.getProductId())
                        .productName(productInfo.getProductName())
                        .productCategory(productInfo.getProductCategory())
                        .productPrice(productInfo.getProductPrice())
                        .quantity(orderEntity.getQuantity())
                        .totalCost(orderEntity.getTotalCost())
                        .createdBy(orderEntity.getCreatedBy())
                        .createdAt(orderEntity.getCreatedAt())
                        .updatedAt(orderEntity.getUpdatedAt())
                        .build();
            }

            @Getter
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class PageDetails {

                @Schema(example = "10")
                private int size;

                @Schema(example = "0")
                private int number;

                @Schema(example = "25")
                private long totalElements;

                @Schema(example = "3")
                private int totalPages;

                public static PageDetails from(Page<OrderEntity> orderEntityPage) {
                    return PageDetails.builder()
                            .size(orderEntityPage.getSize())
                            .number(orderEntityPage.getNumber())
                            .totalElements(orderEntityPage.getTotalElements())
                            .totalPages(orderEntityPage.getTotalPages())
                            .build();
                }
            }
        }
    }
}
