package com.project.paymentservice.presentation.controller;

import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.tosspayments.TossPaymentProperties;
import com.project.paymentservice.presentation.controller.docs.PaymentControllerSwagger;
import com.project.paymentservice.presentation.request.ReqPaymentDTO;
import com.project.paymentservice.presentation.request.ReqTossPaymentCancelDTO;
import com.project.paymentservice.presentation.request.ReqTossPaymentConfirmDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController implements PaymentControllerSwagger {

    private final PaymentService paymentService;
    private final TossPaymentProperties tossPaymentProperties;

    @PostMapping
    public ResponseEntity<ResPaymentDTO> processPayment(@Valid @RequestBody ReqPaymentDTO request) {
        ResPaymentDTO response = paymentService.processPayment(
                request.getOrderId(),
                request.getSalesId(),
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/toss/confirm")
    public ResponseEntity<ResPaymentDTO> confirmTossPayment(@Valid @RequestBody ReqTossPaymentConfirmDTO request) {
        ResPaymentDTO response = paymentService.confirmTossPayment(
                request.getPaymentKey(),
                request.getOrderId(),
                request.getSalesId(),
                request.getAmount()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/toss/cancel")
    public ResponseEntity<ResPaymentDTO> cancelTossPayment(@Valid @RequestBody ReqTossPaymentCancelDTO request) {
        ResPaymentDTO response = paymentService.cancelTossPayment(
                request.getSalesId(),
                request.getCancelReason()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/toss/config")
    public ResponseEntity<TossPaymentConfigResponse> getTossPaymentConfig() {
        return ResponseEntity.ok(new TossPaymentConfigResponse(tossPaymentProperties.getClientKey()));
    }

    @GetMapping("/sales/{salesId}")
    public ResponseEntity<ResPaymentDTO> getPaymentBySalesId(@PathVariable Long salesId) {
        ResPaymentDTO response = paymentService.getPaymentBySalesId(salesId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sales/{salesId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Long salesId) {
        ResPaymentDTO payment = paymentService.getPaymentBySalesId(salesId);
        return ResponseEntity.ok(new PaymentStatusResponse(salesId, payment.getPayment().getStatus()));
    }

    @PostMapping("/sales/{salesId}/refund")
    public ResponseEntity<Void> refundPayment(
            @PathVariable Long salesId,
            @RequestParam(defaultValue = "고객 요청") String reason
    ) {
        paymentService.refundPayment(salesId, reason);
        return ResponseEntity.ok().build();
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class PaymentStatusResponse {
        private Long salesId;
        private String status;
    }

    public record TossPaymentConfigResponse(String clientKey) {
    }
}
