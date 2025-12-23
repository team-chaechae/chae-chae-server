package com.project.common.outbox.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@NoRepositoryBean
public interface OutboxRepository<T extends OutboxEntity> extends JpaRepository<T, Long> {

    List<T> findByAggregateIdAndEventTypeAndStatus(
            String aggregateId, String eventType, OutboxStatus status);

    @Query("SELECT o FROM #{#entityName} o WHERE o.status != :successStatus " +
            "AND o.createdAt < :threshold " +
            "AND o.retryCount < :maxRetries " +
            "ORDER BY o.createdAt ASC LIMIT :limit")
    List<T> findMessagesForRetry(
            @Param("successStatus") OutboxStatus successStatus,
            @Param("threshold") LocalDateTime threshold,
            @Param("maxRetries") int maxRetries,
            @Param("limit") int limit
    );

    @Modifying
    @Query("DELETE FROM #{#entityName} o WHERE o.status = :status AND o.processedAt < :before")
    int deleteProcessedMessagesBefore(
            @Param("status") OutboxStatus status,
            @Param("before") LocalDateTime before
    );
}
