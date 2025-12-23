package com.project.paymentservice.domain.repository;

import com.project.paymentservice.domain.model.OutboxEntity;
import com.project.paymentservice.domain.model.OutboxEntity.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    List<OutboxEntity> findByAggregateIdAndEventTypeAndStatus(
        String aggregateId, String eventType, OutboxStatus status);

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
