package com.project.paymentservice.infrastructure.kafka;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.infrastructure.kafka.backpressure.BlockingThreadPoolExecutor;
import com.project.paymentservice.infrastructure.kafka.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * 주문 생성 이벤트 Consumer
 *
 * order-created 이벤트 수신 → 결제 처리 → payment-completed 이벤트 발행
 * 재고 차감 없이 바로 결제 진행 (새 플로우)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final PaymentService paymentService;
    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;

    @KafkaListener(
            topics = "order-created",
            groupId = "payment-order-group",
            containerFactory = "orderCreatedListenerFactory",
            concurrency = "3"
    )
    public void handleOrderCreated(OrderCreatedEvent event, Acknowledgment ack) {
        String orderId = event.getOrderId();
        Long salesId = event.getSalesId();

        try {
            kafkaBackpressureExecutor.execute(() -> {
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

                    // 재고 차감 없이 바로 결제 처리 (items 포함하여 payment-completed 이벤트 발행)
                    paymentService.processPayment(
                            orderId,
                            salesId,
                            event.getTotalAmount(),
                            items
                    );

                    log.info("[결제 처리 완료] orderId: {}, salesId: {}", orderId, salesId);
                    ack.acknowledge();

                } catch (Exception e) {
                    log.error("[결제 처리 실패] orderId: {}, salesId: {}, 에러: {}",
                            orderId, salesId, e.getMessage());
                    // ack하지 않아 offset commit을 막고 재처리 가능 상태로 둔다.
                    // 결제 처리는 salesId 기반 멱등성으로 중복 처리를 방어한다.
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[백프레셔] 작업 거부 - orderId: {}, 에러: {}", orderId, e.getMessage());
            // ack 안함 → 재처리
        }
    }
}
