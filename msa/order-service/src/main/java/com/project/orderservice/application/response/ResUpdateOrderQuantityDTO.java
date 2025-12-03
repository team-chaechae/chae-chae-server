package com.project.orderservice.application.response;

import com.project.orderservice.domain.model.OrderEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResUpdateOrderQuantityDTO {

    private Order order;

    public static ResUpdateOrderQuantityDTO from(OrderEntity orderEntity) {
        return ResUpdateOrderQuantityDTO.builder()
                .order(Order.from(orderEntity))
                .build();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Order {

        @Schema(example = "1")
        private Long orderId;

        @Schema(example = "20")
        private Integer quantity;

        @Schema(example = "6100")
        private Integer productPrice;

        @Schema(example = "122000")
        private Integer totalCost;

        @Schema(example = "APPROVED")
        private String status;

        private LocalDateTime updatedAt;

        public static Order from(OrderEntity orderEntity) {
            return Order.builder()
                    .orderId(orderEntity.getId())
                    .quantity(orderEntity.getQuantity())
                    .productPrice(orderEntity.getProductInfo().getProductPrice())
                    .totalCost(orderEntity.getTotalCost())
                    .status(orderEntity.getStatus().name())
                    .updatedAt(orderEntity.getUpdatedAt())
                    .build();
        }
    }
}
