package com.project.paymentservice.application.service;

import com.project.paymentservice.application.response.ResPaymentDTO;

public interface PaymentService {

    ResPaymentDTO processPayment(String orderId, Long salesId, Integer amount);

    ResPaymentDTO getPaymentBySalesId(Long salesId);

    void completePayment(Long salesId);

    void failPayment(Long salesId, String reason);

    void cancelPayment(Long salesId, String reason);

    void refundPayment(Long salesId, String reason);
}
