package com.project.paymentservice.presentation.controller.docs;

import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.presentation.controller.PaymentController.TossPaymentConfigResponse;
import com.project.paymentservice.presentation.request.ReqPaymentDTO;
import com.project.paymentservice.presentation.request.ReqTossPaymentCancelDTO;
import com.project.paymentservice.presentation.request.ReqTossPaymentConfirmDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Payment", description = "결제 API")
public interface PaymentControllerSwagger {

    @Operation(summary = "결제 처리", description = "주문에 대한 결제를 처리합니다.")
    ResponseEntity<ResPaymentDTO> processPayment(ReqPaymentDTO request);

    @Operation(summary = "토스페이먼츠 결제 승인", description = "토스페이먼츠 결제 인증 완료 후 paymentKey로 결제를 최종 승인합니다.")
    ResponseEntity<ResPaymentDTO> confirmTossPayment(ReqTossPaymentConfirmDTO request);

    @Operation(summary = "토스페이먼츠 결제 취소", description = "저장된 paymentKey로 토스페이먼츠 결제를 전액 취소하고 환불 이벤트를 발행합니다.")
    ResponseEntity<ResPaymentDTO> cancelTossPayment(ReqTossPaymentCancelDTO request);

    @Operation(summary = "토스페이먼츠 클라이언트 설정 조회", description = "결제창 SDK 초기화에 필요한 공개 클라이언트 키를 조회합니다.")
    ResponseEntity<TossPaymentConfigResponse> getTossPaymentConfig();

    @Operation(summary = "결제 조회", description = "주문 ID로 결제 정보를 조회합니다.")
    ResponseEntity<ResPaymentDTO> getPaymentBySalesId(Long salesId);

    @Operation(summary = "환불 처리", description = "결제를 환불 처리합니다.")
    ResponseEntity<Void> refundPayment(Long salesId, String reason);
}
