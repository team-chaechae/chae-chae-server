package com.project.inventoryservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Column(name = "message_key", length = 100)
    private String messageKey;

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
    public OutboxEntity(String aggregateType, String aggregateId, String eventType,
                        String payload, String topic, String messageKey) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.topic = topic;
        this.messageKey = messageKey;
        this.status = OutboxStatus.INIT;
        this.retryCount = 0;
    }

    public static OutboxEntity create(String aggregateType, String aggregateId, String eventType,
                                       String payload, String topic, String messageKey) {
        return OutboxEntity.builder()
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payload)
            .topic(topic)
            .messageKey(messageKey)
            .build();
    }

    public void markAsSendSuccess() {
        this.status = OutboxStatus.SEND_SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsSendFail(String errorMessage) {
        this.status = OutboxStatus.SEND_FAIL;
        this.retryCount++;
        this.errorMessage = truncateErrorMessage(errorMessage);
    }

    private String truncateErrorMessage(String message) {
        if (message == null) return null;
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public boolean canRetry(int maxRetries) {
        return this.retryCount < maxRetries;
    }

    public enum OutboxStatus {
        INIT,
        SEND_SUCCESS,
        SEND_FAIL
    }
}
