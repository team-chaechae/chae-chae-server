package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.service.InventoryDeductionService;
import com.project.inventoryservice.infrastructure.alert.SlackAlertService;
import com.project.inventoryservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

/**
 * 결제 완료 이벤트 Consumer
 *
 * 결제 완료 → 재고 차감 → 성공/실패 처리
 *
 * DLQ 패턴:
 * - 성공: ack
 * - 실패: 예외 throw → DlqErrorHandler가 DLQ로 발행
 * - 백프레셔 거부: ack 안함 → Kafka 재전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedEventConsumer {

    private static final String TOPIC = "payment-completed";

    private final InventoryDeductionService inventoryDeductionService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SlackAlertService slackAlertService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "inventory-payment-group",
            containerFactory = "paymentCompletedListenerFactory",
            concurrency = "3"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        var previousMdc = MDC.getCopyOfContextMap();
        long startNanos = System.nanoTime();

        try {
            MDC.put("orderId", orderId);
            MDC.put("salesId", String.valueOf(salesId));
            var mdcContext = MDC.getCopyOfContextMap();
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                } else {
                    MDC.clear();
                }
                try {
                inventoryDeductionService.processPaymentCompleted(event);
                } finally {
                    MDC.clear();
                }
            }, kafkaBackpressureExecutor);

            future.get();
            ack.acknowledge();
            meterRegistry.counter("inventory.payment_completed.consumed").increment();
            meterRegistry.timer("inventory.payment_completed.processing")
                    .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, 에러: {}", orderId, e.getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("백프레셔 작업 거부 - orderId: %s", orderId), e);
        } catch (ExecutionException e) {
            log.error("[재고 차감 처리 실패 → DLQ] orderId: {}, salesId: {}, 에러: {}",
                    orderId, salesId, e.getCause().getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("재고 차감 실패 → DLQ - orderId: %s, salesId: %s", orderId, salesId),
                    (Exception) e.getCause());
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            slackAlertService.sendKafkaErrorAlert(TOPIC, "인터럽트 발생", e);
            throw new RuntimeException(e);
        } finally {
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            } else {
                MDC.clear();
            }
        }
    }
}
