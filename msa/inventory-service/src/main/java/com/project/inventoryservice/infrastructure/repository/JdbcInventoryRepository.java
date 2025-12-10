package com.project.inventoryservice.infrastructure.repository;

import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC Template 기반 Repository
 * JPA IDENTITY 전략의 배치 INSERT 제한을 우회
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcInventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 인벤토리 히스토리를 벌크로 삽입합니다. (InventoryEntity 사용)
     */
    public int bulkInsert(List<InventoryEntity> inventoryEntities) {
        if (inventoryEntities == null || inventoryEntities.isEmpty()) {
            return 0;
        }

        String sql = "INSERT INTO inventory (product_id, quantity, change_type, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();

        for (InventoryEntity entity : inventoryEntities) {
            batchArgs.add(new Object[]{
                entity.getProductId(),
                entity.getQuantity(),
                entity.getChangeType() != null ? entity.getChangeType().name() : "RECEIVE",
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
            });
        }

        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        return results.length;
    }

    /**
     * Inventory 히스토리 배치 INSERT (InventoryEvent 사용)
     */
    public void batchInsertInventory(List<InventoryEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO inventory (product_id, quantity, change_type, created_at, updated_at) " +
                     "VALUES (?, ?, ?, NOW(), NOW())";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                InventoryEvent event = events.get(i);
                ps.setLong(1, event.getProductId());
                ps.setInt(2, event.getQuantity());
                ps.setString(3, event.getChangeType());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });

        log.debug("[배치 INSERT] inventory 테이블 - {} 건", events.size());
    }

    /**
     * Stock 테이블 배치 UPDATE
     * 같은 productId에 대한 변경량을 합산하여 한 번에 업데이트
     */
    public void batchUpdateStock(List<InventoryEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        // productId별로 변경량 합산
        Map<Long, Integer> stockChanges = new HashMap<>();
        for (InventoryEvent event : events) {
            stockChanges.merge(event.getProductId(), event.getQuantity(), Integer::sum);
        }

        String sql = "UPDATE stock SET quantity = quantity + ? WHERE product_id = ?";

        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(stockChanges.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, Integer> entry = entries.get(i);
                ps.setInt(1, entry.getValue());
                ps.setLong(2, entry.getKey());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.debug("[배치 UPDATE] stock 테이블 - {} 건 (원본 이벤트 {} 건)",
                  stockChanges.size(), events.size());
    }

    /**
     * Inventory INSERT + Stock UPDATE를 한 트랜잭션으로 처리
     */
    @Transactional
    public void batchProcess(List<InventoryEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        log.info("[배치 처리 시작] {} 건", events.size());
        long startTime = System.currentTimeMillis();

        batchInsertInventory(events);
        batchUpdateStock(events);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[배치 처리 완료] {} 건, 소요시간: {}ms", events.size(), elapsed);
    }
}
