package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.service.InventoryDeductionService;
import com.project.inventoryservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

/**
 * 결제 완료 이벤트 Consumer
 *
 * 결제 완료 → 재고 차감 → 성공/실패 처리
 * 비즈니스 로직은 InventoryDeductionService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private final InventoryDeductionService inventoryDeductionService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "inventory-payment-group",
            containerFactory = "paymentCompletedListenerFactory",
            concurrency = "3"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        try {
            kafkaBackpressureExecutor.execute(() -> {
                try {
                    inventoryDeductionService.processPaymentCompleted(event);
                    ack.acknowledge();
                } catch (Exception e) {
                    log.error("[재고 차감 처리 실패] orderId: {}, salesId: {}, 에러: {}",
                            orderId, salesId, e.getMessage());
                    ack.acknowledge();  // 중복 처리 방지
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, 에러: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }
}
