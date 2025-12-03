package com.project.chaechaeserver.inventory.service;

import com.project.chaechaeserver.application.service.inventory.InventoryService;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 커넥션 생명주기 테스트
 *
 * 청크(배치) 단위로 커넥션이 획득/반납되는지 확인
 */
@SpringBootTest
class ConnectionLifecycleTest {

    private static final Logger log = LoggerFactory.getLogger(ConnectionLifecycleTest.class);

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("청크 단위로 커넥션이 획득되고 반납되는지 확인")
    void testConnectionLifecyclePerBatch() {
        // HikariCP 통계 가져오기
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

        log.info("=== 테스트 시작 ===");
        log.info("초기 커넥션 풀 상태:");
        logPoolStatus(poolMXBean);

        // 100개 상품 입고 (배치 크기 100 → 1개 배치)
        List<ReqBulkCreateInventoryDTO.Inventory> inventories = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            inventories.add(
                ReqBulkCreateInventoryDTO.Inventory.builder()
                    .productId(i % 23 + 1)  // 상품 ID 1~23 순환
                    .quantity(10)
                    .build()
            );
        }

        ReqBulkCreateInventoryDTO dto = ReqBulkCreateInventoryDTO.builder()
            .inventory(inventories)
            .build();

        log.info("\n=== 입고 처리 시작 (100개 상품, 1개 배치) ===");

        // 입고 처리
        inventoryService.createInventoryForBulk(dto);

        log.info("\n=== 입고 처리 완료 ===");
        log.info("최종 커넥션 풀 상태:");
        logPoolStatus(poolMXBean);

        // 잠시 대기 (커넥션 반납 확인)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("\n=== 100ms 대기 후 ===");
        log.info("커넥션 풀 상태 (반납 확인):");
        logPoolStatus(poolMXBean);

        log.info("\n=== 결론 ===");
        log.info("- Active: 사용 중인 커넥션");
        log.info("- Idle: 대기 중인 커넥션 (반납된 커넥션)");
        log.info("- Waiting: 커넥션 대기 중인 스레드");
        log.info("→ 배치가 끝나면 Active가 0이 되고 Idle이 증가해야 함");
    }

    @Test
    @DisplayName("여러 배치 처리 시 각 배치마다 커넥션 반납 확인")
    void testConnectionLifecycleMultipleBatches() {
        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();

        log.info("=== 여러 배치 처리 테스트 ===");
        log.info("300개 상품 입고 → 배치 크기 100 → 3개 배치");

        // 300개 상품 입고
        List<ReqBulkCreateInventoryDTO.Inventory> inventories = new ArrayList<>();
        for (long i = 1; i <= 300; i++) {
            inventories.add(
                ReqBulkCreateInventoryDTO.Inventory.builder()
                    .productId(i % 23 + 1)
                    .quantity(10)
                    .build()
            );
        }

        ReqBulkCreateInventoryDTO dto = ReqBulkCreateInventoryDTO.builder()
            .inventory(inventories)
            .build();

        log.info("\n초기 상태:");
        logPoolStatus(poolMXBean);

        // 입고 처리
        inventoryService.createInventoryForBulk(dto);

        log.info("\n처리 완료 후:");
        logPoolStatus(poolMXBean);

        // 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("\n100ms 대기 후:");
        logPoolStatus(poolMXBean);

        log.info("\n=== 예상 결과 ===");
        log.info("배치 1 완료 → 커넥션 반납 → 배치 2 시작 (같은 커넥션 재사용 가능)");
        log.info("배치 2 완료 → 커넥션 반납 → 배치 3 시작 (같은 커넥션 재사용 가능)");
        log.info("배치 3 완료 → 커넥션 반납");
        log.info("→ 최종적으로 Active = 0, Idle이 증가");
    }

    private void logPoolStatus(HikariPoolMXBean poolMXBean) {
        log.info("  - Total Connections: {}", poolMXBean.getTotalConnections());
        log.info("  - Active Connections: {} (사용 중) 🔴", poolMXBean.getActiveConnections());
        log.info("  - Idle Connections: {} (대기 중, 반납됨) 🟢", poolMXBean.getIdleConnections());
        log.info("  - Threads Waiting: {} (커넥션 대기) ⏳", poolMXBean.getThreadsAwaitingConnection());
    }
}
