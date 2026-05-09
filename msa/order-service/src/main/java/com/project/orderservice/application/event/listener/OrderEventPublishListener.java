package com.project.orderservice.application.event.listener;

import com.project.orderservice.application.event.OrderCreatedInternalEvent;
import com.project.orderservice.application.event.service.OrderEventSendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * AFTER_COMMIT 리스너
 * 트랜잭션 커밋 후 즉시 Kafka로 발행 (낮은 지연시간)
 * 실패해도 Outbox Relay가 재발행 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublishListener {

    private final OrderEventSendService sendService;

    @Async(OrderEventAsyncConfig.ORDER_EVENT_ASYNC_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishOrderCreated(OrderCreatedInternalEvent event) {
        sendService.sendOrderCreated(event);
    }
}
