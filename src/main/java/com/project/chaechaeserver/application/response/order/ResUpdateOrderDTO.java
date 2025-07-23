package com.project.chaechaeserver.application.response.order;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.ProductInfo;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResUpdateOrderDTO {

    private Order order;

    public static ResUpdateOrderDTO from(OrderEntity orderEntity) {
        return ResUpdateOrderDTO.builder()
                .order(Order.from(orderEntity))
                .build();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Order {

        private Long orderId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private Integer totalCost;
        private String status;
        private LocalDateTime updatedAt;

        public static Order from(OrderEntity orderEntity) {
            ProductInfo productInfo = orderEntity.getProductInfo();
            return Order.builder()
                    .orderId(orderEntity.getId())
                    .productId(productInfo.getProductId())
                    .productName(productInfo.getProductName())
                    .quantity(orderEntity.getQuantity())
                    .totalCost(orderEntity.getTotalCost())
                    .status(orderEntity.getStatus().name())
                    .updatedAt(orderEntity.getUpdatedAt())
                    .build();
        }
    }
}
