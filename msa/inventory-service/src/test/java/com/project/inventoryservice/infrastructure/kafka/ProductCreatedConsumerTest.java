package com.project.inventoryservice.infrastructure.kafka;

import com.project.inventoryservice.application.service.StockCacheService;
import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.repository.StockRepository;
import com.project.inventoryservice.infrastructure.kafka.dto.ProductCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상품 생성 이벤트 → Stock 생성 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductCreatedConsumerTest {

    @Autowired
    private ProductCreatedConsumer productCreatedConsumer;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockCacheService stockCacheService;

    private static final Long TEST_PRODUCT_ID = 888888L;

    @BeforeEach
    void setUp() {
        // 테스트 전 기존 데이터 정리
        stockRepository.findByProductId(TEST_PRODUCT_ID)
                .ifPresent(stock -> stockRepository.delete(stock));
    }

    @Test
    @DisplayName("상품 생성 이벤트 수신 시 Stock 레코드가 생성되어야 한다 (초기 재고 0)")
    void consumeProductCreated_shouldCreateStockWithZeroQuantity() {
        // given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(TEST_PRODUCT_ID)
                .productName("테스트 상품")
                .category("테스트 카테고리")
                .price(10000)
                .createdAt(LocalDateTime.now())
                .build();

        // Stock이 없는 상태에서 시작
        Optional<StockEntity> beforeStock = stockRepository.findByProductId(TEST_PRODUCT_ID);
        assertThat(beforeStock).isEmpty();

        // when - 이벤트 처리
        productCreatedConsumer.consumeProductCreated(event);

        // then - Stock 테이블에 재고 0으로 생성되어야 함
        Optional<StockEntity> afterStock = stockRepository.findByProductId(TEST_PRODUCT_ID);
        assertThat(afterStock).isPresent();
        assertThat(afterStock.get().getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("상품 생성 이벤트 수신 시 Redis 캐시도 생성되어야 한다")
    void consumeProductCreated_shouldCreateRedisCache() {
        // given
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(TEST_PRODUCT_ID)
                .productName("테스트 상품")
                .category("테스트 카테고리")
                .price(10000)
                .createdAt(LocalDateTime.now())
                .build();

        // when - 이벤트 처리
        productCreatedConsumer.consumeProductCreated(event);

        // then - Redis 캐시에도 재고 0으로 등록되어야 함
        Integer cachedStock = stockCacheService.getCurrentStock(TEST_PRODUCT_ID);
        assertThat(cachedStock).isEqualTo(0);
    }

    @Test
    @DisplayName("이미 존재하는 상품의 이벤트는 무시해야 한다")
    void consumeProductCreated_existingProduct_shouldIgnore() {
        // given - 이미 재고가 있는 상품
        Integer existingQuantity = 50;
        stockRepository.save(new StockEntity(TEST_PRODUCT_ID, existingQuantity));

        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .productId(TEST_PRODUCT_ID)
                .productName("테스트 상품")
                .category("테스트 카테고리")
                .price(10000)
                .createdAt(LocalDateTime.now())
                .build();

        // when - 이벤트 처리
        productCreatedConsumer.consumeProductCreated(event);

        // then - 기존 재고가 유지되어야 함 (덮어쓰기 안 됨)
        Optional<StockEntity> stock = stockRepository.findByProductId(TEST_PRODUCT_ID);
        assertThat(stock).isPresent();
        assertThat(stock.get().getQuantity()).isEqualTo(existingQuantity);
    }
}
