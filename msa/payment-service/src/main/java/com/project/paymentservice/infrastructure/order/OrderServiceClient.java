package com.project.paymentservice.infrastructure.order;

import com.project.paymentservice.infrastructure.order.dto.OrderSalesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OrderServiceProperties.class)
public class OrderServiceClient implements OrderSalesClient {

    private final OrderServiceProperties properties;

    @Override
    public OrderSalesResponse.SalesDetail getSales(Long salesId) {
        try {
            OrderSalesResponse response = RestClient.create(properties.getBaseUrl())
                    .get()
                    .uri("/api/sales/{salesId}", salesId)
                    .retrieve()
                    .body(OrderSalesResponse.class);

            if (response == null || response.data() == null || response.data().sales() == null) {
                throw new OrderSalesClientException("판매 정보를 조회할 수 없습니다. salesId: " + salesId);
            }
            return response.data().sales();
        } catch (RestClientException e) {
            throw new OrderSalesClientException("판매 정보 조회에 실패했습니다. salesId: " + salesId, e);
        }
    }
}
