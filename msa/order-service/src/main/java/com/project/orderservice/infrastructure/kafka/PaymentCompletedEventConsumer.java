package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.infrastructure.alert.SlackAlertService;
import com.project.orderservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
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
 * payment-completed 이벤트 수신 → 주문 상태 COMPLETED로 변경
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

    private final SalesService salesService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SlackAlertService slackAlertService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "order-payment-group",
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
                    log.info("[결제 완료 이벤트 수신] orderId: {}, salesId: {}, totalAmount: {}",
                            orderId, salesId, event.getTotalAmount());

                    salesService.completeSales(salesId, orderId);
                } finally {
                    MDC.clear();
                }
            }, kafkaBackpressureExecutor);

            future.get();
            ack.acknowledge();
            meterRegistry.counter("order.payment_completed.consumed").increment();
            meterRegistry.timer("order.payment_completed.processing")
                    .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, error: {}", orderId, e.getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("백프레셔 작업 거부 - orderId: %s", orderId), e);
        } catch (ExecutionException e) {
            log.error("[주문 상태 변경 실패 → DLQ] orderId: {}, salesId: {}, error: {}",
                    orderId, salesId, e.getCause().getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("주문 상태 변경 실패 → DLQ - orderId: %s, salesId: %s", orderId, salesId),
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
