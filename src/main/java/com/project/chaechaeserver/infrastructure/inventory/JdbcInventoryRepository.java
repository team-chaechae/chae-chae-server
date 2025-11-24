package com.project.chaechaeserver.infrastructure.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcInventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 인벤토리 히스토리를 벌크로 삽입합니다.
     * @param inventoryEntities 삽입할 인벤토리 엔티티 리스트
     * @return 삽입된 row 수
     */
    public int bulkInsert(List<InventoryEntity> inventoryEntities) {
        if (inventoryEntities == null || inventoryEntities.isEmpty()) {
            return 0;
        }

        String sql = """

            INSERT INTO inventory (product_id, quantity, change_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();

        for (InventoryEntity entity : inventoryEntities) {
            batchArgs.add(new Object[]{
                entity.getProductId(),              // product_id (Long)
                entity.getQuantity(),               // quantity (Integer)
                entity.getChangeType() != null ? entity.getChangeType().name() : "RECEIVE",  // change_type (기본값 RECEIVE)
                Timestamp.valueOf(now),             // created_at
                Timestamp.valueOf(now)              // updated_at
            });
        }

        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        return results.length;
    }
}
