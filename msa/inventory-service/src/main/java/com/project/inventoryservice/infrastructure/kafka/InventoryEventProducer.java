package com.project.inventoryservice.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 재고 이벤트 Kafka Producer
 * 재고 변경 이벤트를 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventProducer {

    private static final String TOPIC = "inventory-events";
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    /**
     * 재고 이벤트 발행 성공을 확인한다.
     *
     * @param event 재고 변경 이벤트
     */
    public void publish(InventoryEvent event) {
        // 라운드 로빈 분배 (순서 보장 불필요 - 원자적 증감 연산)
        String key = null;
        Long productId = event.getProductId();
        String eventId = event.getEventId();

        try {
            log.debug("[Kafka 발행 시작] 상품ID: {}, 변경량: {}, 타입: {}, EventID: {}",
                productId, event.getQuantity(), event.getChangeType(), eventId);

            SendResult<String, InventoryEvent> result = kafkaTemplate.send(TOPIC, key, event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("[Kafka 발행 성공] 재고 이벤트 - 상품ID: {}, 변경량: {}, 타입: {}, Partition: {}, Offset: {}, EventID: {}",
                productId,
                event.getQuantity(),
                event.getChangeType(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(),
                eventId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[Kafka 발행 인터럽트] 재고 이벤트 - 상품ID: {}, EventID: {}", productId, eventId, e);
            throw new RuntimeException("재고 이벤트 발행 인터럽트 - 상품ID: " + productId, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            log.error("[Kafka 발행 실패] 재고 이벤트 - 상품ID: {}, EventID: {}, 에러 타입: {}, 메시지: {}",
                    productId, eventId, cause.getClass().getSimpleName(), cause.getMessage());
            log.error("[Kafka 발행 실패] 스택트레이스", cause);
            throw new RuntimeException("재고 이벤트 발행 실패 - 상품ID: " + productId, cause);
        } catch (TimeoutException e) {
            log.error("[Kafka 발행 타임아웃] 재고 이벤트 - 상품ID: {}, EventID: {}", productId, eventId, e);
            throw new RuntimeException("재고 이벤트 발행 타임아웃 - 상품ID: " + productId, e);
        } catch (Exception e) {
            log.error("[Kafka 발행 예외] 재고 이벤트 - 상품ID: {}, EventID: {}, 에러 타입: {}, 메시지: {}",
                productId, eventId, e.getClass().getSimpleName(), e.getMessage());
            log.error("[Kafka 발행 예외] 스택트레이스", e);
            throw new RuntimeException("재고 이벤트 발행 실패 - 상품ID: " + productId, e);
        }
    }
}
