package com.project.chaechaeserver.application.response.order;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResCreateOrderPostDTO {

  private Order order;

  public static ResCreateOrderPostDTO from(OrderEntity orderEntity) {
    return ResCreateOrderPostDTO.builder()
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
    private String productCategory;
    private Integer quantity;
    private Integer unitCost;
    private Integer totalCost;
    private String status;

    public static Order from(OrderEntity orderEntity) {
      return Order.builder()
          .orderId(orderEntity.getId())
          .productName(orderEntity.getProduct().getName())
          .productCategory(orderEntity.getProduct().getCategory())
          .quantity(orderEntity.getQuantity())
          .unitCost(orderEntity.getUnitCost())
          .totalCost(orderEntity.getTotalCost())
          .status(orderEntity.getStatus().name())
          .build();
    }
  }

}
