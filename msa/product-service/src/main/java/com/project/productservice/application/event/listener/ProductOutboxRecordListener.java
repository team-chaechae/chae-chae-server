package com.project.productservice.application.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.productservice.application.event.ProductCreatedInternalEvent;
import com.project.productservice.domain.model.OutboxEntity;
import com.project.productservice.domain.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * BEFORE_COMMIT 리스너
 * 트랜잭션 커밋 전에 Outbox 테이블에 이벤트를 기록
 * 도메인 로직과 같은 트랜잭션으로 묶여서 원자성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductOutboxRecordListener {

    private static final String AGGREGATE_TYPE_PRODUCT = "PRODUCT";
    private static final String EVENT_TYPE_PRODUCT_CREATED = "PRODUCT_CREATED";

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void recordOutbox(ProductCreatedInternalEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEntity outbox = OutboxEntity.create(
                AGGREGATE_TYPE_PRODUCT,
                event.getProductId(),
                EVENT_TYPE_PRODUCT_CREATED,
                payload
            );

            outboxRepository.save(outbox);
            log.debug("[Outbox Record] 상품 생성 이벤트 기록 - productId: {}", event.getProductId());

        } catch (JsonProcessingException e) {
            log.error("[Outbox Record] 이벤트 직렬화 실패 - productId: {}", event.getProductId(), e);
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }
}
