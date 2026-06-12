package com.project.deliveryservice.domain.repository;

import com.project.deliveryservice.domain.model.OutboxEntity;
import com.project.deliveryservice.domain.model.OutboxEntity.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OutboxRepository extends JpaRepository<OutboxEntity, Long> {

    @Query("SELECT o FROM OutboxEntity o WHERE o.status IN :retryStatuses "
            + "AND o.createdAt < :threshold "
            + "AND o.retryCount < :maxRetries "
            + "ORDER BY o.createdAt ASC")
    List<OutboxEntity> findMessagesForRetry(
            @Param("retryStatuses") List<OutboxStatus> retryStatuses,
            @Param("threshold") LocalDateTime threshold,
            @Param("maxRetries") int maxRetries,
            Pageable pageable
    );

    default List<OutboxEntity> findMessagesForRetry(
            LocalDateTime threshold,
            int maxRetries,
            int limit
    ) {
        return findMessagesForRetry(
                List.of(OutboxStatus.INIT, OutboxStatus.SEND_FAIL),
                threshold,
                maxRetries,
                PageRequest.of(0, limit)
        );
    }

    @Modifying
    @Query("DELETE FROM OutboxEntity o WHERE o.status = :status AND o.processedAt < :before")
    int deleteProcessedMessagesBefore(
            @Param("status") OutboxStatus status,
            @Param("before") LocalDateTime before
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o "
            + "SET o.status = :successStatus, o.processedAt = :processedAt "
            + "WHERE o.eventId = :eventId AND o.status = :currentStatus")
    int updateStatusSuccessByEventId(
            @Param("eventId") String eventId,
            @Param("currentStatus") OutboxStatus currentStatus,
            @Param("successStatus") OutboxStatus successStatus,
            @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o "
            + "SET o.status = :failStatus, o.retryCount = o.retryCount + 1, "
            + "o.errorMessage = :errorMessage, o.processedAt = :processedAt "
            + "WHERE o.eventId = :eventId AND o.status = :currentStatus")
    int updateStatusFailByEventId(
            @Param("eventId") String eventId,
            @Param("currentStatus") OutboxStatus currentStatus,
            @Param("failStatus") OutboxStatus failStatus,
            @Param("errorMessage") String errorMessage,
            @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o "
            + "SET o.status = :successStatus, o.processedAt = :processedAt "
            + "WHERE o.id = :id")
    int updateStatusSuccessById(
            @Param("id") Long id,
            @Param("successStatus") OutboxStatus successStatus,
            @Param("processedAt") LocalDateTime processedAt
    );

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity o "
            + "SET o.status = :failStatus, o.retryCount = :retryCount, "
            + "o.errorMessage = :errorMessage, o.processedAt = :processedAt "
            + "WHERE o.id = :id")
    int updateStatusFailByIdWithRetryCount(
            @Param("id") Long id,
            @Param("failStatus") OutboxStatus failStatus,
            @Param("retryCount") int retryCount,
            @Param("errorMessage") String errorMessage,
            @Param("processedAt") LocalDateTime processedAt
    );
}
