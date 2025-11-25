package com.project.chaechaeserver.application.response.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO.OrderCustomer.OrderCustomerBuilder;
import com.project.chaechaeserver.domain.model.order.OrderCustomerEntity;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.OrderItemEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResCreateOrderCustomerPostDTO {

    private OrderCustomer orderCustomer;


    public static ResCreateOrderCustomerPostDTO from(OrderCustomerEntity orderCustomerEntity,
        List<OrderItemEntity> orderItemEntity) {
        return ResCreateOrderCustomerPostDTO.builder()
            .orderCustomer(OrderCustomer.from(orderCustomerEntity, orderItemEntity))
            .build();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderCustomer {

        private Long customerId;
        private Long orderId;
        private String productName;
        private Integer totalQuantity;
        private Integer totalPrice;


        private static String formatProductDisplayName(List<OrderItemEntity> orderItemEntities) {
            String firstProductName = orderItemEntities.get(0).getProductName();

            int remainingCount = orderItemEntities.size() - 1;

            if (remainingCount > 0) {
                return firstProductName + " 외 " + remainingCount + "건";
            } else {
                return firstProductName;
            }
        }
            public static OrderCustomer from (OrderCustomerEntity
            orderCustomerEntity, List < OrderItemEntity > orderItemEntities){

                String orderProductInfo = OrderCustomer.formatProductDisplayName(orderItemEntities);

                return OrderCustomer.builder()
                    .customerId(orderCustomerEntity.getCustomerId())
                    .orderId(orderCustomerEntity.getId())
                    .productName(orderProductInfo)
                    .totalQuantity(orderCustomerEntity.getTotalQuantity())
                    .totalPrice(orderCustomerEntity.getTotalPrice())
                    .build();
            }

    }
}

