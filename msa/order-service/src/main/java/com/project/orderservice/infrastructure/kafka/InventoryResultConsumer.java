package com.project.orderservice.infrastructure.kafka;

import com.project.orderservice.application.service.SalesServiceImpl;
import com.project.orderservice.infrastructure.kafka.dto.InventoryResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 재고 처리 결과 Consumer (Inventory → Order)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryResultConsumer {

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
}
