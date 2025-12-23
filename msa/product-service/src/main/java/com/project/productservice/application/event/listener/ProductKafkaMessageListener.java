package com.project.productservice.application.event.listener;

import com.project.productservice.application.event.ProductCreatedInternalEvent;
import com.project.productservice.domain.model.OutboxEntity;
import com.project.productservice.domain.model.OutboxEntity.OutboxStatus;
import com.project.productservice.domain.repository.OutboxRepository;
import com.project.productservice.infrastructure.kafka.ProductEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * AFTER_COMMIT 리스너
 * 트랜잭션 커밋 후에 Kafka로 메시지 발행
 * 비동기로 실행되어 메인 로직 성능에 영향 없음
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductKafkaMessageListener {

    private static final String EVENT_TYPE_PRODUCT_CREATED = "PRODUCT_CREATED";

    private final ProductEventProducer productEventProducer;
    private final OutboxRepository outboxRepository;

    @Async("outboxAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendKafkaMessage(ProductCreatedInternalEvent event) {
        log.info("[Kafka Send] 상품 생성 메시지 발행 시도 - productId: {}", event.getProductId());

        try {
            productEventProducer.publishProductCreatedSync(
                event.getProductId(),
                event.getProductName(),
                event.getCategory(),
                event.getPrice()
            );

            updateOutboxStatus(event.getProductId(), true, null);
            log.info("[Kafka Send] 상품 생성 메시지 발행 성공 - productId: {}", event.getProductId());

        } catch (Exception e) {
            log.error("[Kafka Send] 상품 생성 메시지 발행 실패 - productId: {}, error: {}",
                event.getProductId(), e.getMessage());

            updateOutboxStatus(event.getProductId(), false, e.getMessage());
        }
    }

    @Transactional
    public void updateOutboxStatus(Long productId, boolean success, String errorMessage) {
        List<OutboxEntity> outboxList = outboxRepository.findByAggregateIdAndEventTypeAndStatus(
            productId, EVENT_TYPE_PRODUCT_CREATED, OutboxStatus.INIT);

        for (OutboxEntity outbox : outboxList) {
            if (success) {
                outbox.markAsSendSuccess();
            } else {
                outbox.markAsSendFail(errorMessage);
            }
        }
    }
}
