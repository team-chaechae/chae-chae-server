package com.project.orderservice.infrastructure.client;

import com.project.orderservice.infrastructure.client.dto.PaymentDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    /**
     * 결제 처리
     */
    @PostMapping("/api/payment")
    PaymentDTO.Response processPayment(@RequestBody PaymentDTO.Request request);

    /**
     * 결제 상태 조회
     */
    @GetMapping("/api/payment/sales/{salesId}/status")
    PaymentStatusResponse getPaymentStatus(@PathVariable Long salesId);

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    class PaymentStatusResponse {
        private Long salesId;
        private String status;
    }
}
