package com.project.orderservice.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service")
public interface PaymentFeignClient {

    @PostMapping("/api/payment/sales/{salesId}/refund")
    void refundPayment(
            @PathVariable Long salesId,
            @RequestParam String reason
    );
}
