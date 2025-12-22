package com.project.paymentservice.infrastructure.kafka;

import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.InventoryFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

/**
 * 재고 차감 실패 이벤트 Consumer
 *
 * inventory-failed 이벤트 수신 → 결제 환불 처리 → payment-refunded 이벤트 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedEventConsumer {

    private final PaymentService paymentService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;

    @KafkaListener(
            topics = "inventory-failed",
            groupId = "payment-inventory-failed-group",
            containerFactory = "inventoryFailedListenerFactory",
            concurrency = "3"
    )
    public void handleInventoryFailed(InventoryFailedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        String reason = event.getReason();

        try {
            kafkaBackpressureExecutor.execute(() -> {
                try {
                    log.info("[재고 차감 실패 이벤트 수신] orderId: {}, salesId: {}, reason: {}",
                            orderId, salesId, reason);

                    // 결제 환불 처리 (payment-refunded 이벤트 발행됨)
                    paymentService.refundPayment(salesId, reason);

                    log.info("[환불 처리 완료] orderId: {}, salesId: {}", orderId, salesId);
                    ack.acknowledge();

                } catch (Exception e) {
                    log.error("[환불 처리 실패] orderId: {}, salesId: {}, 에러: {}",
                            orderId, salesId, e.getMessage());
                    ack.acknowledge();  // 중복 환불 방지
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, 에러: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }
}
