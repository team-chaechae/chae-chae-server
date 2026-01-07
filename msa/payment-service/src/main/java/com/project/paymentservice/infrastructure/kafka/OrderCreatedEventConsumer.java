package com.project.paymentservice.infrastructure.kafka;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.alert.SlackAlertService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.OrderCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * 주문 생성 이벤트 Consumer
 *
 * order-created 이벤트 수신 → 결제 처리 → payment-completed 이벤트 발행
 *
 * DLQ 패턴:
 * - 성공: ack
 * - 실패: 예외 throw → DlqErrorHandler가 DLQ로 발행
 * - 백프레셔 거부: ack 안함 → Kafka 재전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private static final String TOPIC = "order-created";

    private final PaymentService paymentService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;
    private final SlackAlertService slackAlertService;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics = "order-created",
            groupId = "payment-order-group",
            containerFactory = "orderCreatedListenerFactory",
            concurrency = "3"
    )
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();
        var previousMdc = MDC.getCopyOfContextMap();
        long startNanos = System.nanoTime();

        try {
            MDC.put("orderId", orderId);
            MDC.put("salesId", String.valueOf(salesId));
            var mdcContext = MDC.getCopyOfContextMap();
            // 백프레셔 적용 + 예외 전파를 위해 CompletableFuture 사용
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                } else {
                    MDC.clear();
                }
                try {
                log.info("[주문 생성 이벤트 수신] orderId: {}, salesId: {}, amount: {}",
                        orderId, salesId, event.getTotalAmount());

                // items 변환 (OrderCreatedEvent.OrderItem → PaymentCompletedInternalEvent.OrderItem)
                List<PaymentCompletedInternalEvent.OrderItem> items = null;
                if (event.getItems() != null) {
                    items = event.getItems().stream()
                            .map(item -> PaymentCompletedInternalEvent.OrderItem.builder()
                                    .productId(item.getProductId())
                                    .productName(item.getProductName())
                                    .quantity(item.getQuantity())
                                    .price(item.getPrice())
                                    .build())
                            .collect(Collectors.toList());
                }

                // 결제 처리 (실패 시 예외 발생)
                paymentService.processPayment(
                        orderId,
                        salesId,
                        event.getTotalAmount(),
                        items
                );

                log.info("[결제 처리 완료] orderId: {}, salesId: {}", orderId, salesId);
                } finally {
                    MDC.clear();
                }
            }, kafkaBackpressureExecutor);

            // 완료 대기 (예외 발생 시 ExecutionException으로 래핑됨)
            future.get();

            // 성공 시에만 ack
            ack.acknowledge();
            meterRegistry.counter("payment.order_created.consumed").increment();
            meterRegistry.timer("payment.order_created.processing")
                    .record(System.nanoTime() - startNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        } catch (RejectedExecutionException e) {
            // 백프레셔: ack 안함 → Kafka가 재전송
            log.warn("[백프레셔] 작업 거부 - orderId: {}, 에러: {}", orderId, e.getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("백프레셔 작업 거부 - orderId: %s", orderId), e);
        } catch (ExecutionException e) {
            // 비즈니스 로직 예외: DlqErrorHandler로 전파
            log.error("[결제 처리 실패 → DLQ] orderId: {}, salesId: {}, 에러: {}",
                    orderId, salesId, e.getCause().getMessage());
            slackAlertService.sendKafkaErrorAlert(TOPIC,
                    String.format("결제 처리 실패 → DLQ - orderId: %s, salesId: %s", orderId, salesId),
                    (Exception) e.getCause());
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[인터럽트] orderId: {}", orderId);
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
