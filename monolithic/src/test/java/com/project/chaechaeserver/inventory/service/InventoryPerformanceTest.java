package com.project.chaechaeserver.inventory.service;

import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.service.inventory.InventoryService;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootTest
class InventoryPerformanceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        System.out.println("기존 DB 상품을 사용하여 테스트 준비 완료");
    }

    @Test
    @Transactional
    @Rollback(false)
    void 재고_대량_생성_성능_테스트_1000개() {
        // Given - 기존 DB 상품들을 1000개까지 반복해서 사용
        ReqBulkCreateInventoryDTO dto = createBulkInventoryRequestWithExistingProducts(1000);

        System.out.println("=== 재고 1000개 생성 성능 테스트 시작 ===");

        // When - 성능 측정 시작
        long startTime = System.currentTimeMillis();

        ResBulkCreateInventoryPostDTO result = inventoryService.createInventoryForBulk(dto);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        System.out.println("=== 재고 1000개 생성 성능 테스트 결과 ===");
        System.out.println("실행 시간: " + executionTime + "ms");
        System.out.println("생성된 재고 수: " + result.getInventory().size());
        System.out.println("평균 처리 시간 (개당): " + String.format("%.3f", executionTime / 1000.0) + "ms");
        System.out.println("TPS (초당 처리량): " + String.format("%.1f", (1000 * 1000.0 / executionTime)) + " items/sec");

        System.out.println("=== 테스트 완료 ===");
    }

    @Test
    @Transactional
    @Rollback(false)
    void 재고_대량_생성_배치_크기별_성능_비교() {
        int[] batchSizes = {100, 250, 500, 1000};

        System.out.println("=== 배치 크기별 성능 비교 ===");
        System.out.printf("%-10s | %-12s | %-15s | %-10s%n",
            "배치크기", "실행시간(ms)", "개당평균(ms)", "TPS");
        System.out.println("------------------------------------------------------");

        for (int batchSize : batchSizes) {
            // Given
            ReqBulkCreateInventoryDTO dto = createBulkInventoryRequestWithExistingProducts(batchSize);

            // When
            long startTime = System.currentTimeMillis();
            ResBulkCreateInventoryPostDTO result = inventoryService.createInventoryForBulk(dto);
            long endTime = System.currentTimeMillis();

            long executionTime = endTime - startTime;
            double avgTime = executionTime / (double) batchSize;
            double tps = (batchSize * 1000.0 / executionTime);

            // 결과 출력
            System.out.printf("%-10d | %-12d | %-15.3f | %-10.1f%n",
                batchSize, executionTime, avgTime, tps);
        }

        System.out.println("=== 배치 크기별 성능 비교 완료 ===");
    }

    @Test
    @Transactional
    @Rollback(false)
    void 메모리_사용량_모니터링_테스트() {
        // Given
        Runtime runtime = Runtime.getRuntime();

        // 가비지 컬렉션 실행
        System.gc();
        try {
            Thread.sleep(100); // GC 완료 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        ReqBulkCreateInventoryDTO dto = createBulkInventoryRequestWithExistingProducts(1000);

        // When
        System.out.println("=== 메모리 사용량 모니터링 시작 ===");
        long startTime = System.currentTimeMillis();
        ResBulkCreateInventoryPostDTO result = inventoryService.createInventoryForBulk(dto);
        long endTime = System.currentTimeMillis();

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterMemory - beforeMemory;

        // Then
        System.out.println("=== 메모리 사용량 분석 결과 ===");
        System.out.println("처리 시간: " + (endTime - startTime) + "ms");
        System.out.println("처리 전 메모리: " + formatBytes(beforeMemory));
        System.out.println("처리 후 메모리: " + formatBytes(afterMemory));
        System.out.println("사용된 메모리: " + formatBytes(memoryUsed));
        System.out.println("항목당 메모리: " + formatBytes(Math.max(0, memoryUsed / 1000)));
        System.out.println("총 힙 메모리: " + formatBytes(runtime.totalMemory()));
        System.out.println("사용 가능 메모리: " + formatBytes(runtime.freeMemory()));
        System.out.println("처리된 재고 수: " + result.getInventory().size());
        System.out.println("=== 메모리 모니터링 완료 ===");
    }

    @Test
    @Transactional
    @Rollback(false)
    void 연속_처리_성능_테스트() {
        System.out.println("=== 연속 처리 성능 테스트 (100개씩 10회) ===");

        long totalTime = 0;
        int totalProcessed = 0;

        for (int round = 1; round <= 10; round++) {
            ReqBulkCreateInventoryDTO dto = createBulkInventoryRequestWithExistingProducts(100);

            long startTime = System.currentTimeMillis();
            ResBulkCreateInventoryPostDTO result = inventoryService.createInventoryForBulk(dto);
            long endTime = System.currentTimeMillis();

            long roundTime = endTime - startTime;
            totalTime += roundTime;
            totalProcessed += result.getInventory().size();

            System.out.printf("Round %2d: %4dms (처리: %3d개) | 누적TPS: %6.1f%n",
                round, roundTime, result.getInventory().size(),
                (totalProcessed * 1000.0 / totalTime));
        }

        System.out.println("------------------------------------------------------");
        System.out.printf("총 처리 시간: %dms%n", totalTime);
        System.out.printf("총 처리 항목: %d개%n", totalProcessed);
        System.out.printf("전체 평균 TPS: %.1f items/sec%n", (totalProcessed * 1000.0 / totalTime));
        System.out.printf("라운드별 평균 시간: %dms%n", totalTime / 10);
        System.out.println("=== 연속 처리 성능 테스트 완료 ===");
    }

    // 헬퍼 메소드들
    private ReqBulkCreateInventoryDTO createBulkInventoryRequestWithExistingProducts(int count) {
        // 기존 DB의 상품 ID들 (1~23)을 순환하면서 1000개까지 생성
        List<ReqBulkCreateInventoryDTO.Inventory> inventories = IntStream.range(0, count)
            .mapToObj(i -> {
                Long productId = (long) ((i % 23) + 1); // 1~23 순환
                Integer quantity = 100 + (i % 50); // 100~149 사이의 수량

                return ReqBulkCreateInventoryDTO.Inventory.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build();
            })
            .collect(Collectors.toList());

        return ReqBulkCreateInventoryDTO.builder()
            .inventory(inventories)
            .build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "0 bytes";
        if (bytes < 1024) return bytes + " bytes";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}