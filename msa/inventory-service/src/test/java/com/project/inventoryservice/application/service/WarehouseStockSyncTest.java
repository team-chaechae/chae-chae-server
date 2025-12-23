package com.project.inventoryservice.application.service;

import com.project.inventoryservice.application.service.warehouse.WarehouseBatchProcessor;
import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.StockEntity;
import com.project.inventoryservice.domain.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 입고 시 Stock 테이블 동기화 테스트
 * Note: @Transactional 제거 - REQUIRES_NEW 트랜잭션과 충돌 방지
 */
@SpringBootTest
@ActiveProfiles("test")
class WarehouseStockSyncTest {

    @Autowired
    private WarehouseBatchProcessor warehouseBatchProcessor;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockCacheService stockCacheService;

    private static final Long TEST_PRODUCT_ID = 999999L;

    @BeforeEach
    void setUp() {
        // 테스트 전 기존 데이터 정리
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        // 테스트 후 데이터 정리
        cleanupTestData();
    }

    private void cleanupTestData() {
        stockRepository.findByProductId(TEST_PRODUCT_ID).ifPresent(stockRepository::delete);
        stockRepository.findByProductId(TEST_PRODUCT_ID + 1).ifPresent(stockRepository::delete);
        stockRepository.findByProductId(TEST_PRODUCT_ID + 2).ifPresent(stockRepository::delete);
    }

    @Test
    @DisplayName("입고 처리 시 Stock 테이블에 재고가 증가해야 한다")
    void receiveInventory_shouldIncreaseStockTable() {
        // given
        Long productId = TEST_PRODUCT_ID;
        Integer receiveQuantity = 100;

        // Stock이 없는 상태에서 시작
        Optional<StockEntity> beforeStock = stockRepository.findByProductId(productId);
        assertThat(beforeStock).isEmpty();

        // when - 입고 처리
        List<InventoryEntity> result = warehouseBatchProcessor.processBatchReceive(
                List.of(productId),
                List.of(receiveQuantity)
        );

        // then - Stock 테이블에 재고가 생성되어야 함
        Optional<StockEntity> afterStock = stockRepository.findByProductId(productId);
        assertThat(afterStock).isPresent();
        assertThat(afterStock.get().getQuantity()).isEqualTo(receiveQuantity);

        // Inventory 히스토리도 생성되어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(productId);
        assertThat(result.get(0).getQuantity()).isEqualTo(receiveQuantity);
    }

    @Test
    @DisplayName("기존 재고가 있는 상품에 입고 시 재고가 누적되어야 한다")
    void receiveInventory_shouldAccumulateStock() {
        // given
        Long productId = TEST_PRODUCT_ID;
        Integer initialStock = 50;
        Integer receiveQuantity = 100;

        // 초기 재고 설정
        stockRepository.save(new StockEntity(productId, initialStock));

        // when - 입고 처리
        warehouseBatchProcessor.processBatchReceive(
                List.of(productId),
                List.of(receiveQuantity)
        );

        // then - 재고가 누적되어야 함 (50 + 100 = 150)
        Optional<StockEntity> afterStock = stockRepository.findByProductId(productId);
        assertThat(afterStock).isPresent();
        assertThat(afterStock.get().getQuantity()).isEqualTo(initialStock + receiveQuantity);
    }

    @Test
    @DisplayName("여러 상품 동시 입고 시 각각의 Stock이 증가해야 한다")
    void receiveInventory_multipleProducts_shouldIncreaseEachStock() {
        // given
        List<Long> productIds = List.of(TEST_PRODUCT_ID, TEST_PRODUCT_ID + 1, TEST_PRODUCT_ID + 2);
        List<Integer> quantities = List.of(100, 200, 300);

        // when - 다중 상품 입고 처리
        List<InventoryEntity> result = warehouseBatchProcessor.processBatchReceive(productIds, quantities);

        // then - 각 상품의 Stock이 생성되어야 함
        assertThat(result).hasSize(3);

        for (int i = 0; i < productIds.size(); i++) {
            Optional<StockEntity> stock = stockRepository.findByProductId(productIds.get(i));
            assertThat(stock).isPresent();
            assertThat(stock.get().getQuantity()).isEqualTo(quantities.get(i));
        }
    }

    @Test
    @DisplayName("입고 시 Redis 캐시도 갱신되어야 한다")
    void receiveInventory_shouldUpdateRedisCache() {
        // given
        Long productId = TEST_PRODUCT_ID;
        Integer receiveQuantity = 100;

        // when - 입고 처리
        warehouseBatchProcessor.processBatchReceive(
                List.of(productId),
                List.of(receiveQuantity)
        );

        // then - Redis 캐시에도 재고가 반영되어야 함
        Integer cachedStock = stockCacheService.getCurrentStock(productId);
        assertThat(cachedStock).isEqualTo(receiveQuantity);
    }
}
