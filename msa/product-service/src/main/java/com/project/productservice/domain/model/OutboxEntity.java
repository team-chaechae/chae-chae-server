package com.project.productservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Table(name = "outbox",
    indexes = {
        @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
    })
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Builder
    public OutboxEntity(String aggregateType, Long aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.INIT;
        this.retryCount = 0;
    }

    public static OutboxEntity create(String aggregateType, Long aggregateId, String eventType, String payload) {
        return OutboxEntity.builder()
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payload)
            .build();
    }

    public void markAsSendSuccess() {
        this.status = OutboxStatus.SEND_SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsSendFail(String errorMessage) {
        this.status = OutboxStatus.SEND_FAIL;
        this.retryCount++;
        this.errorMessage = errorMessage;
    }

    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries;
    }

    /**
     * 29CM 방식 상태값
     * - INIT: 이벤트 발행 등록 (outbox에 처음 기록될 때)
     * - SEND_SUCCESS: 이벤트 발행 성공
     * - SEND_FAIL: 이벤트 발행 실패 (카프카 전송 실패)
     */
    public enum OutboxStatus {
        INIT,
        SEND_SUCCESS,
        SEND_FAIL
    }
}
