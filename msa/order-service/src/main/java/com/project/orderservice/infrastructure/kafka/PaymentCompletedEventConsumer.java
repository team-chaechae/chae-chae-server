package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private final SalesRepository salesRepository;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "order-payment-group",
            containerFactory = "paymentCompletedListenerFactory"
    )
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        log.info("[결제 완료 이벤트 수신] orderId: {}, salesId: {}, totalAmount: {}",
                orderId, salesId, event.getTotalAmount());

        try {
            SalesEntity sales = salesRepository.findSalesBySalesId(salesId);

            // 이미 완료된 주문이면 스킵 (멱등성)
            if (sales.isCompleted()) {
                log.info("[주문 상태 변경 스킵 - 이미 완료] orderId: {}, salesId: {}", orderId, salesId);
                ack.acknowledge();
                return;
            }

            // 주문 상태를 COMPLETED로 변경
            sales.complete();

            log.info("[주문 상태 완료] orderId: {}, salesId: {}, status: {}",
                    orderId, salesId, sales.getStatus());

            // 처리 성공 시 ACK
            ack.acknowledge();

        } catch (Exception e) {
            log.error("[주문 상태 변경 실패] orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getMessage(), e);
            // ACK하지 않으면 재시도됨
            throw e;
        }
    }
}
