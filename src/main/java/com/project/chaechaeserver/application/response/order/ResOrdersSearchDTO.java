package com.project.chaechaeserver.application.response.order;

import com.project.chaechaeserver.application.response.order.ResOrdersSearchDTO.OrdersPage.Orders.PageDetails;
import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.ProductInfo;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
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

      private Long orderId;
      private StatusType status;
      private Long productId;
      private String productName;
      private String productCategory;
      private Integer productPrice;
      private Integer quantity;
      private Integer totalCost;
      private LocalDateTime updatedAt;
      private String createdBy;

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
            .updatedAt(orderEntity.getUpdatedAt())
            .createdBy(orderEntity.getCreatedBy())
            .build();
      }

      @Getter
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class PageDetails {

        private int size;
        private int number;
        private long totalElements;
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
