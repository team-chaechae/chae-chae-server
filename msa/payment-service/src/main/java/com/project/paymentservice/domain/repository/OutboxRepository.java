package com.project.paymentservice.domain.repository;

import com.project.paymentservice.domain.model.OutboxEntity;
import com.project.paymentservice.domain.model.OutboxEntity.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o " +
           "SET o.status = :successStatus, o.processedAt = :processedAt " +
           "WHERE o.aggregateId = :aggregateId AND o.eventType = :eventType AND o.status = :currentStatus")
    int updateStatusSuccessByAggregateIdAndEventType(
        @Param("aggregateId") String aggregateId,
        @Param("eventType") String eventType,
        @Param("currentStatus") OutboxStatus currentStatus,
        @Param("successStatus") OutboxStatus successStatus,
        @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o " +
           "SET o.status = :failStatus, o.retryCount = o.retryCount + 1, o.errorMessage = :errorMessage " +
           "WHERE o.aggregateId = :aggregateId AND o.eventType = :eventType AND o.status = :currentStatus")
    int updateStatusFailByAggregateIdAndEventType(
        @Param("aggregateId") String aggregateId,
        @Param("eventType") String eventType,
        @Param("currentStatus") OutboxStatus currentStatus,
        @Param("failStatus") OutboxStatus failStatus,
        @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o " +
           "SET o.status = :successStatus, o.processedAt = :processedAt " +
           "WHERE o.id = :id")
    int updateStatusSuccessById(
        @Param("id") Long id,
        @Param("successStatus") OutboxStatus successStatus,
        @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o " +
           "SET o.status = :failStatus, o.retryCount = o.retryCount + 1, o.errorMessage = :errorMessage " +
           "WHERE o.id = :id")
    int updateStatusFailById(
        @Param("id") Long id,
        @Param("failStatus") OutboxStatus failStatus,
        @Param("errorMessage") String errorMessage
    );
}
