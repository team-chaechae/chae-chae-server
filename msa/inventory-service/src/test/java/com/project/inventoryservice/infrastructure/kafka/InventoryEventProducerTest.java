package com.project.inventoryservice.infrastructure.kafka;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryEventProducer")
class InventoryEventProducerTest {

    @Mock
    private KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @Test
    @DisplayName("Kafka send future가 실패하면 발행 실패 예외를 던진다")
    void publish_WhenKafkaSendFails_ThrowsException() {
        // given
        InventoryEventProducer producer = new InventoryEventProducer(kafkaTemplate);
        InventoryEvent event = InventoryEvent.builder()
                .eventId("event-1")
                .productId(1999L)
                .quantity(-2)
                .changeType("ORDER_DECREASE")
                .occurredAt(LocalDateTime.now())
                .currentStock(8)
                .build();
        given(kafkaTemplate.send(eq("inventory-events"), any(), eq(event)))
                .willReturn(CompletableFuture.failedFuture(new RuntimeException("broker 장애")));

        // when & then
        assertThatThrownBy(() -> producer.publish(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("재고 이벤트 발행 실패")
                .hasRootCauseMessage("broker 장애");
    }
}
