package com.project.orderservice.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 재고 처리 결과 Consumer (Inventory → Order)
 *
 * 현재 동기 HTTP 방식으로 전환되어 비활성화됨
 * 활성화: application.yml에 kafka.saga.enabled=true 설정
 *
 * TODO: 결제 서비스 추가 시 Saga 패턴 재활성화 검토
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.saga.enabled", havingValue = "true", matchIfMissing = false)
public class InventoryResultConsumer {

    // 동기 HTTP 전환으로 비활성화
    // Saga 패턴 재활성화 시 아래 코드 복원 필요
    /*
    private final SalesServiceImpl salesService;

    @KafkaListener(
            topics = "inventory-result",
            groupId = "order-saga-group",
            containerFactory = "inventoryResultListenerFactory"
    )
    public void consumeInventoryResult(InventoryResultEvent event) {
        log.info("[Kafka 수신] 재고 처리 결과 - salesId: {}, status: {}",
                event.getSalesId(), event.getStatus());

        if (event.isSuccess()) {
            salesService.completeSales(event.getSalesId());
        } else {
            salesService.cancelSales(event.getSalesId(), event.getFailureReason());
        }
    }
    */
}
