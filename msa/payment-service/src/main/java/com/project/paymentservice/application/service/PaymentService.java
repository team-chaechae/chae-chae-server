package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.response.ResPaymentDTO;

import java.util.List;

public interface PaymentService {

    ResPaymentDTO processPayment(String orderId, Long salesId, Integer amount);

    ResPaymentDTO processPayment(String orderId, Long salesId, Integer amount, List<PaymentCompletedInternalEvent.OrderItem> items);

    ResPaymentDTO confirmTossPayment(String paymentKey, String orderId, Long salesId, Integer amount);

    ResPaymentDTO cancelTossPayment(Long salesId, String cancelReason);

    ResPaymentDTO getPaymentBySalesId(Long salesId);

    void completePayment(Long salesId);

    void failPayment(Long salesId, String reason);

    void cancelPayment(Long salesId, String reason);

    void refundPayment(Long salesId, String reason);
}
