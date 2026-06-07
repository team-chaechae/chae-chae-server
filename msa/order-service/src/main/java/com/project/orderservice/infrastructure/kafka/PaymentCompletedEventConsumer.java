package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionException;

/**
 * 결제 완료 이벤트 Consumer
 *
 * payment-completed 이벤트 수신 → 주문 상태 COMPLETED로 변경
 * 비즈니스 로직은 SalesService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private static final String TOPIC = "payment-completed";

    private final SalesService salesService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SlackAlertService slackAlertService;

    @KafkaListener(
            topics = TOPIC,
            groupId = "order-payment-group",
            containerFactory = "paymentCompletedListenerFactory",
            concurrency = "3"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        try {
            kafkaBackpressureExecutor.execute(() -> {
                try {
                    log.info("[결제 완료 이벤트 수신] orderId: {}, salesId: {}, totalAmount: {}",
                            orderId, salesId, event.getTotalAmount());

                    salesService.completeSales(salesId, orderId);
                    ack.acknowledge();
                } catch (Exception e) {
                    log.error("[주문 상태 변경 실패] orderId: {}, salesId: {}, error: {}",
                            orderId, salesId, e.getMessage());
                    slackAlertService.sendKafkaErrorAlert(TOPIC,
                            "주문 상태 변경 실패 - orderId: " + orderId + ", salesId: " + salesId, e);
                    // ack하지 않아 offset commit을 막고 재처리 가능 상태로 둔다.
                    // 주문 상태 변경은 현재 상태 확인으로 중복 이벤트를 방어한다.
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, error: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }
}
