package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.PaymentOrchestrationService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 이벤트 Consumer
 *
 * payment-completed 이벤트 수신 → 결제 완료 오케스트레이션 실행
 * 비즈니스 로직은 PaymentOrchestrationService에 위임
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private static final String TOPIC = "payment-completed";

    private final PaymentOrchestrationService paymentOrchestrationService;
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
            kafkaBackpressureExecutor.execute(() -> processPaymentCompleted(event, ack, orderId, salesId));
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, error: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }

    private void processPaymentCompleted(
            PaymentCompletedEvent event,
            Acknowledgment ack,
            String orderId,
            Long salesId
    ) {
        try {
            log.info("[결제 완료 이벤트 수신] orderId: {}, salesId: {}, totalAmount: {}",
                    orderId, salesId, event.getTotalAmount());

            paymentOrchestrationService.handlePaymentCompleted(event);
            ack.acknowledge();
        } catch (Exception e) {
            handleProcessingFailure(orderId, salesId, e);
        }
    }

    private void handleProcessingFailure(String orderId, Long salesId, Exception e) {
        log.error("[결제 완료 오케스트레이션 실패] orderId: {}, salesId: {}",
                orderId, salesId, e);
        slackAlertService.sendKafkaErrorAlert(TOPIC,
                "결제 완료 오케스트레이션 실패 - orderId: " + orderId + ", salesId: " + salesId, e);
        // ack하지 않아 offset commit을 막고 재처리 가능 상태로 둔다.
        // 오케스트레이션은 salesId 기준 상태 저장으로 중복 이벤트를 방어한다.
    }
}
