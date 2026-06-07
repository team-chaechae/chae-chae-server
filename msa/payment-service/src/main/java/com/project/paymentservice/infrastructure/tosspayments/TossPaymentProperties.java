package com.project.paymentservice.infrastructure.tosspayments;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "toss.payments")
public class TossPaymentProperties {

    private String baseUrl = "https://api.tosspayments.com";
    private String clientKey = "";
    private String secretKey = "";
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(10);
}
