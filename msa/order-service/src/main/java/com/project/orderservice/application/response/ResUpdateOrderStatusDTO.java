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
public class ResUpdateOrderStatusDTO {

    private Order order;

    public static ResUpdateOrderStatusDTO from(OrderEntity orderEntity) {
        return ResUpdateOrderStatusDTO.builder()
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

        @Schema(example = "COMPLETED")
        private String status;

        private LocalDateTime updatedAt;

        public static Order from(OrderEntity orderEntity) {
            return Order.builder()
                    .orderId(orderEntity.getId())
                    .status(orderEntity.getStatus().name())
                    .updatedAt(orderEntity.getUpdatedAt())
                    .build();
        }
    }
}
