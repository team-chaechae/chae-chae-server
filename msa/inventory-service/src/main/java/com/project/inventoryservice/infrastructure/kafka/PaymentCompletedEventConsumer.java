package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.saga.StockReservationLuaExecutor;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private final StockReservationLuaExecutor luaExecutor;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "inventory-payment-group",
            containerFactory = "paymentCompletedListenerFactory"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        log.info("[결제 완료 이벤트 수신] orderId: {}, salesId: {}", orderId, salesId);

        // orderId로 모든 예약 확정
        luaExecutor.confirmAllReservations(orderId);

        log.info("[재고 확정 완료] orderId: {}, salesId: {}", orderId, salesId);
    }
}
