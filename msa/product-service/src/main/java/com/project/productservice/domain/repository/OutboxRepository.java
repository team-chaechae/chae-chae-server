package com.project.productservice.domain.repository;

import com.project.productservice.domain.model.OutboxEntity;
import com.project.productservice.domain.model.OutboxEntity.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    List<OutboxEntity> findByAggregateIdAndEventTypeAndStatus(
        Long aggregateId, String eventType, OutboxStatus status);

    /**
     * 재발행 대상 조회 (29CM 방식)
     * - SEND_SUCCESS가 아니면서 created_at이 현 시간 기준으로 특정 시간 이상 지난 것들
     */
    @Query("SELECT o FROM OutboxEntity o WHERE o.status != :successStatus " +
           "AND o.createdAt < :threshold " +
           "AND o.retryCount < :maxRetries " +
           "ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEntity> findMessagesForRetry(
        @Param("successStatus") OutboxStatus successStatus,
        @Param("threshold") LocalDateTime threshold,
        @Param("maxRetries") int maxRetries,
        @Param("limit") int limit
    );

    @Modifying
    @Query("DELETE FROM OutboxEntity o WHERE o.status = :status AND o.processedAt < :before")
    int deleteProcessedMessagesBefore(
        @Param("status") OutboxStatus status,
        @Param("before") LocalDateTime before
    );
}
