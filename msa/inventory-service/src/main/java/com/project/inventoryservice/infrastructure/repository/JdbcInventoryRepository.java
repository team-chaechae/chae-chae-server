package com.project.inventoryservice.infrastructure.repository;

import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryStatus;
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
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

        String sql = "INSERT INTO inventory (product_id, quantity, change_type, order_id, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();

        for (InventoryEntity entity : inventoryEntities) {
            batchArgs.add(new Object[]{
                entity.getProductId(),
                entity.getQuantity(),
                entity.getChangeType() != null ? entity.getChangeType().name() : "RECEIVE",
                entity.getOrderId(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
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

        String sql = "INSERT INTO inventory (product_id, quantity, change_type, order_id, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, NOW(), NOW())";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                InventoryEvent event = events.get(i);
                ps.setLong(1, event.getProductId());
                ps.setInt(2, event.getQuantity());
                ps.setString(3, event.getChangeType());

                if (event.getOrderId() != null) {
                    ps.setString(4, event.getOrderId());
                } else {
                    ps.setNull(4, Types.VARCHAR);
                }

                if (event.getStatus() != null) {
                    ps.setString(5, event.getStatus());
                } else {
                    ps.setNull(5, Types.VARCHAR);
                }
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        });

        log.debug("[배치 INSERT] inventory 테이블 - {} 건", events.size());
    }

    /**
     * Inventory 이벤트 스토어에 배치 INSERT
     * stock 테이블은 StockSyncScheduler가 주기적으로 동기화
     */
    @Transactional
    public void batchProcess(List<InventoryEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        log.info("[배치 처리 시작] inventory 이벤트 {} 건", events.size());
        long startTime = System.currentTimeMillis();

        // inventory 테이블에만 INSERT (이벤트 스토어)
        // stock 테이블 동기화는 StockSyncScheduler가 담당
        batchInsertInventory(events);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[배치 처리 완료] inventory {} 건 저장, 소요시간: {}ms", events.size(), elapsed);
    }

    /**
     * orderId로 예약 재고를 CONFIRMED 상태로 확정합니다.
     */
    public int confirmByOrderId(String orderId) {
        String sql = "UPDATE inventory " +
                     "SET status = 'CONFIRMED', updated_at = NOW() " +
                     "WHERE order_id = ? AND change_type = 'ORDER_DECREASE' AND status = 'RESERVED'";

        int updated = jdbcTemplate.update(sql, orderId);
        log.info("[CONFIRM UPDATE] orderId: {}, 변경 건수: {}", orderId, updated);
        return updated;
    }

    /**
     * orderId로 예약 재고를 CANCELLED 상태로 변경합니다.
     */
    public int cancelByOrderId(String orderId) {
        String sql = "UPDATE inventory " +
                     "SET status = 'CANCELLED', updated_at = NOW() " +
                     "WHERE order_id = ? AND change_type = 'ORDER_DECREASE' AND status = 'RESERVED'";

        int updated = jdbcTemplate.update(sql, orderId);
        log.info("[CANCEL UPDATE] orderId: {}, 변경 건수: {}", orderId, updated);
        return updated;
    }
}
