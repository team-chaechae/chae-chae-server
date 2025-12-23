package com.project.paymentservice.presentation.controller.docs;

import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.presentation.request.ReqPaymentDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payment", description = "결제 API")
public interface PaymentControllerSwagger {

    @Operation(summary = "결제 처리", description = "주문에 대한 결제를 처리합니다.")
    ResponseEntity<ResPaymentDTO> processPayment(ReqPaymentDTO request);

    @Operation(summary = "결제 조회", description = "주문 ID로 결제 정보를 조회합니다.")
    ResponseEntity<ResPaymentDTO> getPaymentBySalesId(Long salesId);

    @Operation(summary = "환불 처리", description = "결제를 환불 처리합니다.")
    ResponseEntity<Void> refundPayment(Long salesId, String reason);
}
