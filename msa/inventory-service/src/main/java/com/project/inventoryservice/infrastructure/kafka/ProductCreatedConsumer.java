package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.service.StockCacheService;
import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.repository.StockRepository;
import com.project.inventoryservice.infrastructure.kafka.dto.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 생성 이벤트 Consumer
 * Product Service에서 상품 생성 시 → Stock 레코드 생성 (초기 재고 0)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCreatedConsumer {

    private final StockRepository stockRepository;
    private final StockCacheService stockCacheService;

    @KafkaListener(
            topics = "product-created",
            groupId = "inventory-product-group",
            containerFactory = "productCreatedListenerFactory"
    )
    @Transactional
    public void consumeProductCreated(ProductCreatedEvent event) {
        log.info("[Kafka 수신] 상품 생성 이벤트 - productId: {}, name: {}",
                event.getProductId(), event.getProductName());

        try {
            // 1. Stock 테이블에 레코드 생성 (초기 재고 0)
            if (stockRepository.findByProductId(event.getProductId()).isEmpty()) {
                StockEntity stock = new StockEntity(event.getProductId(), 0);
                stockRepository.save(stock);
                log.info("[Stock 생성] productId: {}, 초기 재고: 0", event.getProductId());

                // 2. Redis 캐시에도 등록 (초기 재고 0)
                stockCacheService.updateStockCache(event.getProductId(), 0);
                log.info("[Redis 캐시 생성] productId: {}, 재고: 0", event.getProductId());
            } else {
                log.info("[Stock 존재] productId: {} - 이미 존재하는 상품", event.getProductId());
            }

        } catch (Exception e) {
            log.error("[Stock 생성 실패] productId: {}, error: {}",
                    event.getProductId(), e.getMessage(), e);
            throw e;
        }
    }
}
