package com.project.paymentservice.infrastructure.order;

import com.project.paymentservice.infrastructure.order.dto.OrderSalesResponse;

public interface OrderSalesClient {

    OrderSalesResponse.SalesDetail getSales(Long salesId);
}
