package com.project.productservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * 상품 이벤트 Producer
 * 상품 생성/수정/삭제 이벤트를 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private static final String TOPIC_PRODUCT_CREATED = "product-created";

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    /**
     * 상품 생성 이벤트 동기 발행
     * Outbox 패턴에서 발행 결과 확인이 필요할 때 사용
     */
    public void publishProductCreatedSync(Long productId, String productName, String category, Integer price) {
        ProductCreatedEvent event = ProductCreatedEvent.of(productId, productName, category, price);
        String key = String.valueOf(productId);

        log.info("[Kafka 발행] 상품 생성 이벤트 - productId: {}, name: {}", productId, productName);

        try {
            kafkaTemplate.send(TOPIC_PRODUCT_CREATED, key, event).get();
            log.info("[Kafka 발행 성공] productId: {}", productId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka 발행 중 인터럽트 발생", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Kafka 발행 실패: " + e.getCause().getMessage(), e.getCause());
        }
    }
}
