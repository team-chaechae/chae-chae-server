package com.project.paymentservice.infrastructure.kafka;

import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.alert.SlackAlertService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.InventoryFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

/**
 * 재고 차감 실패 이벤트 Consumer
 *
 * inventory-failed 이벤트 수신 → 결제 환불 처리 → payment-refunded 이벤트 발행
 *
 * DLQ 패턴:
 * - 성공: ack
 * - 실패: 예외 throw → DlqErrorHandler가 DLQ로 발행
 * - 백프레셔 거부: ack 안함 → Kafka 재전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedEventConsumer {

    private static final String TOPIC = "inventory-failed";

    private final PaymentService paymentService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SlackAlertService slackAlertService;

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
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                log.info("[재고 차감 실패 이벤트 수신] orderId: {}, salesId: {}, reason: {}",
                        orderId, salesId, reason);

                // 결제 환불 처리 (payment-refunded 이벤트 발행됨)
                paymentService.refundPayment(salesId, reason);

                log.info("[환불 처리 완료] orderId: {}, salesId: {}", orderId, salesId);
            }, kafkaBackpressureExecutor);

            future.get();
            ack.acknowledge();

        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, 에러: {}", orderId, e.getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("백프레셔 작업 거부 - orderId: %s", orderId), e);
        } catch (ExecutionException e) {
            log.error("[환불 처리 실패 → DLQ] orderId: {}, salesId: {}, 에러: {}",
                    orderId, salesId, e.getCause().getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("환불 처리 실패 → DLQ - orderId: %s, salesId: %s", orderId, salesId),
                    (Exception) e.getCause());
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            slackAlertService.sendKafkaErrorAlert(TOPIC, "인터럽트 발생", e);
            throw new RuntimeException(e);
        }
    }
}
