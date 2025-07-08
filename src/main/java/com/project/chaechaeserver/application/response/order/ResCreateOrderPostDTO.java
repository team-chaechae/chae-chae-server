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

  private OrderInfo orderInfo;

  public static ResCreateOrderPostDTO from(OrderEntity orderEntity) {
    return ResCreateOrderPostDTO.builder()
        .orderInfo(OrderInfo.from(orderEntity))
        .build();
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class OrderInfo {

    private Long orderId;
    private Long productId;
    private Integer quantity;
    private Integer unitCost;
    private Integer totalCost;
    private String status;

    public static OrderInfo from(OrderEntity orderEntity) {
      return OrderInfo.builder()
          .orderId(orderEntity.getId())
          .productId(orderEntity.getProductId())
          .quantity(orderEntity.getQuantity())
          .unitCost(orderEntity.getUnitCost())
          .totalCost(orderEntity.getTotalCost())
          .status(orderEntity.getStatus().name())
          .build();
    }
  }

}
