package com.project.chaechaeserver.application.response.order;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.ProductInfo;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(example = "1")
    private Long orderId;

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

    @Schema(example = "APPROVED")
    private String status;

    public static Order from(OrderEntity orderEntity) {
      ProductInfo productInfo = orderEntity.getProductInfo();
      return Order.builder()
          .orderId(orderEntity.getId())
          .productId(productInfo.getProductId())
          .productName(productInfo.getProductName())
          .productCategory(productInfo.getProductCategory())
          .productPrice(productInfo.getProductPrice())
          .quantity(orderEntity.getQuantity())
          .totalCost(orderEntity.getTotalCost())
          .status(orderEntity.getStatus().name())
          .build();
    }
  }

}
