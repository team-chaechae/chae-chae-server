package com.project.paymentservice.infrastructure.tosspayments;

import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentConfirmResponse;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentCancelResponse;

public interface TossPaymentClient {

    TossPaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Integer amount, String idempotencyKey);

    TossPaymentCancelResponse cancelPayment(String paymentKey, String cancelReason, String idempotencyKey);
}
