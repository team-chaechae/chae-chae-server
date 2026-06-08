package com.project.paymentservice.infrastructure.order;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "order.service")
public class OrderServiceProperties {

    private String baseUrl = "http://localhost:8084";
}
