package com.project.chaechaeserver.inventory;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.domain.service.redis.RedisInventoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType.AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Disabled;

/**
 * Write-Back 패턴의 정합성 문제를 검증하는 테스트
 *
 * 목적: Redis와 DB 간 데이터 불일치 상황을 재현하고 측정
 *
 * NOTE: 이 테스트는 옛날 API (Product.quantity, RedisInventoryService.decreaseStock 등)를 사용하므로 비활성화됨
 */
@Disabled("재고 관리 아키텍처 변경으로 인해 API가 변경됨. 새로운 아키텍처에 맞게 재작성 필요")
@Slf4j
@SpringBootTest
public class WriteBackConsistencyTest {

    @Autowired
    private RedisInventoryService redisInventoryService;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    private Long testProductId;
    private static final int INITIAL_STOCK = 10000;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 생성 (unique constraint 때문에 타임스탬프 추가)
        String uniqueName = "테스트상품_" + System.currentTimeMillis();
        ProductEntity product = ProductEntity.builder()
                .name(uniqueName)
                .initialQuantity(INITIAL_STOCK)
                .category("테스트 카테고리")
                .productStatusType(AVAILABLE)
                .price(10000)
                .build();
        ProductEntity saved = productsRepository.save(product);
        testProductId = saved.getId();

        // Redis에 초기 재고 설정
        redisInventoryService.setStock(testProductId, INITIAL_STOCK);

        log.info("테스트 준비 완료 - 상품: {}, ID: {}, 초기 재고: {}", uniqueName, testProductId, INITIAL_STOCK);
    }

    @Test
    @DisplayName("동시 주문 시 Redis와 DB 재고 불일치 검증")
    void testConsistencyWithConcurrentOrders() throws InterruptedException {
        int threadCount = 100;
        int ordersPerThread = 10;
        int decreaseAmount = 1;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        log.info("=== 동시 주문 테스트 시작 ===");
        log.info("스레드 수: {}, 주문/스레드: {}, 총 주문: {}",
                threadCount, ordersPerThread, threadCount * ordersPerThread);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ordersPerThread; j++) {
                        try {
                            redisInventoryService.decreaseStock(
                                    testProductId,
                                    decreaseAmount,
                                    (long) (orderId * 1000 + j)
                            );
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long endTime = System.currentTimeMillis();

        log.info("주문 처리 완료 - 소요시간: {}ms", endTime - startTime);
        log.info("성공: {}, 실패: {}", successCount.get(), failureCount.get());

        // 즉시 재고 확인 (동기화 전)
        Integer redisStock = redisInventoryService.getStock(testProductId);
        ProductEntity dbProduct = productsRepository.findProductByProductId(testProductId);
        Integer dbStock = dbProduct.getQuantity();

        int expectedStock = INITIAL_STOCK - (successCount.get() * decreaseAmount);

        log.info("=== 동기화 전 재고 상태 ===");
        log.info("예상 재고: {}", expectedStock);
        log.info("Redis 재고: {}", redisStock);
        log.info("DB 재고: {}", dbStock);
        log.info("불일치: {}", Math.abs(redisStock - dbStock));

        // Redis는 정확해야 함
        assertThat(redisStock).isEqualTo(expectedStock);

        // DB는 아직 동기화 안 됨 (불일치 발생!)
        boolean isInconsistent = !redisStock.equals(dbStock);
        log.warn("정합성 체크: Redis={}, DB={}, 불일치={}", redisStock, dbStock, isInconsistent);

        // 5초 대기 후 동기화 확인
        log.info("5초 대기 중... (스케줄러 동기화 대기)");
        Thread.sleep(6000);

        // 동기화 후 재확인
        dbProduct = productsRepository.findProductByProductId(testProductId);
        Integer dbStockAfterSync = dbProduct.getQuantity();

        log.info("=== 동기화 후 재고 상태 ===");
        log.info("Redis 재고: {}", redisStock);
        log.info("DB 재고: {}", dbStockAfterSync);
        log.info("일치 여부: {}", redisStock.equals(dbStockAfterSync));

        // 동기화 후에는 일치해야 함
        assertThat(dbStockAfterSync).isEqualTo(redisStock);
    }

    @Test
    @DisplayName("스케줄러 동기화 타이밍 이슈 재현")
    void testSchedulerTimingIssue() throws InterruptedException {
        log.info("=== 스케줄러 타이밍 이슈 테스트 시작 ===");

        List<Integer> redisSnapshots = new ArrayList<>();
        List<Integer> dbSnapshots = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();

        ExecutorService orderExecutor = Executors.newFixedThreadPool(10);

        // 15초 동안 지속적으로 주문 발생
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        // 주문 생성 태스크
        scheduler.scheduleAtFixedRate(() -> {
            try {
                redisInventoryService.decreaseStock(testProductId, 1, System.currentTimeMillis());
            } catch (Exception e) {
                // 재고 부족 시 무시
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        // 재고 스냅샷 수집 태스크 (500ms마다)
        scheduler.scheduleAtFixedRate(() -> {
            Integer redis = redisInventoryService.getStock(testProductId);
            ProductEntity db = productsRepository.findProductByProductId(testProductId);
            Integer dbStock = db != null ? db.getQuantity() : null;

            synchronized (redisSnapshots) {
                redisSnapshots.add(redis);
                dbSnapshots.add(dbStock);
                timestamps.add(System.currentTimeMillis());

                int diff = Math.abs(redis - dbStock);
                if (diff > 0) {
                    log.warn("불일치 감지! Redis={}, DB={}, 차이={}", redis, dbStock, diff);
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        // 15초 실행
        Thread.sleep(15000);
        scheduler.shutdown();

        log.info("=== 스냅샷 분석 ===");
        int maxDiff = 0;
        int inconsistencyCount = 0;

        for (int i = 0; i < redisSnapshots.size(); i++) {
            int diff = Math.abs(redisSnapshots.get(i) - dbSnapshots.get(i));
            if (diff > 0) {
                inconsistencyCount++;
                maxDiff = Math.max(maxDiff, diff);
            }
        }

        log.info("총 스냅샷: {}", redisSnapshots.size());
        log.info("불일치 횟수: {}", inconsistencyCount);
        log.info("최대 차이: {}", maxDiff);
        log.info("불일치 비율: {}%", (inconsistencyCount * 100.0 / redisSnapshots.size()));

        assertThat(inconsistencyCount).isGreaterThan(0).describedAs("불일치가 발생해야 함");
    }

    @Test
    @DisplayName("서버 재시작 시뮬레이션 - 데이터 손실 검증")
    void testDataLossOnRestart() {
        log.info("=== 서버 재시작 데이터 손실 테스트 ===");

        // 1. 초기 상태 확인
        Integer initialRedis = redisInventoryService.getStock(testProductId);
        ProductEntity initialDb = productsRepository.findProductByProductId(testProductId);
        log.info("초기 상태 - Redis: {}, DB: {}", initialRedis, initialDb.getQuantity());

        // 2. 대량 주문 발생
        int orderCount = 500;
        for (int i = 0; i < orderCount; i++) {
            redisInventoryService.decreaseStock(testProductId, 1, (long) i);
        }

        Integer redisAfterOrders = redisInventoryService.getStock(testProductId);
        log.info("주문 후 Redis 재고: {}", redisAfterOrders);

        // 3. 동기화 전에 Redis 데이터 강제 삭제 (서버 장애 시뮬레이션)
        log.warn("Redis 데이터 강제 삭제 (서버 장애 시뮬레이션)");
        redisTemplate.delete("inventory:" + testProductId);
        redisTemplate.delete("inventory:dirty:" + testProductId);

        // 4. DB 상태 확인
        ProductEntity dbAfterCrash = productsRepository.findProductByProductId(testProductId);
        log.error("장애 후 DB 재고: {} (손실된 주문: {}개)",
                dbAfterCrash.getQuantity(),
                initialDb.getQuantity() - dbAfterCrash.getQuantity());

        // 5. 데이터 손실 검증
        int expectedStock = INITIAL_STOCK - orderCount;
        int actualLoss = orderCount - (INITIAL_STOCK - dbAfterCrash.getQuantity());

        log.error("=== 데이터 손실 결과 ===");
        log.error("예상 최종 재고: {}", expectedStock);
        log.error("실제 DB 재고: {}", dbAfterCrash.getQuantity());
        log.error("손실된 주문 데이터: {}건", actualLoss);

        assertThat(actualLoss).isGreaterThan(0).describedAs("동기화 전 장애 시 데이터 손실 발생");
    }

    @Test
    @DisplayName("Dirty Flag 제거 타이밍 이슈")
    void testDirtyFlagTimingIssue() throws InterruptedException {
        log.info("=== Dirty Flag 타이밍 이슈 테스트 ===");

        // 주문 발생
        redisInventoryService.decreaseStock(testProductId, 100, 1L);

        // Dirty Flag 확인
        boolean isDirtyBefore = redisInventoryService.isDirty(testProductId);
        log.info("주문 후 Dirty Flag: {}", isDirtyBefore);

        assertThat(isDirtyBefore).isTrue();

        // 스케줄러 동기화 대기
        Thread.sleep(6000);

        // Dirty Flag 제거 확인
        boolean isDirtyAfter = redisInventoryService.isDirty(testProductId);
        log.info("동기화 후 Dirty Flag: {}", isDirtyAfter);

        assertThat(isDirtyAfter).isFalse();

        // 동기화 후 새로운 주문 - 다시 Dirty Flag 설정되어야 함
        redisInventoryService.decreaseStock(testProductId, 50, 2L);
        boolean isDirtyAgain = redisInventoryService.isDirty(testProductId);

        log.info("새 주문 후 Dirty Flag: {}", isDirtyAgain);
        assertThat(isDirtyAgain).isTrue();
    }
}