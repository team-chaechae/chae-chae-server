package com.project.orderservice.domain.model;

import com.project.orderservice.infrastructure.kafka.dto.DeliveryStatusChangedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "sales_delivery_status",
        indexes = {
                @Index(name = "idx_sales_delivery_status_sales_id", columnList = "sales_id", unique = true),
                @Index(name = "idx_sales_delivery_status_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesDeliveryStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_id", nullable = false, unique = true)
    private Long salesId;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesDeliveryStatusType status;

    @Column(name = "tracking_number", length = 80)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "last_event_id", nullable = false, length = 80)
    private String lastEventId;

    @Column(name = "last_event_at", nullable = false)
    private LocalDateTime lastEventAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static SalesDeliveryStatusEntity create(DeliveryStatusChangedEvent event) {
        SalesDeliveryStatusEntity projection = new SalesDeliveryStatusEntity();
        projection.salesId = event.getSalesId();
        projection.apply(event);
        return projection;
    }

    public void applyIfNewer(DeliveryStatusChangedEvent event) {
        if (lastEventAt != null && event.getOccurredAt().isBefore(lastEventAt)) {
            return;
        }
        apply(event);
    }

    private void apply(DeliveryStatusChangedEvent event) {
        this.deliveryId = event.getDeliveryId();
        this.userId = event.getUserId();
        this.status = event.getStatus();
        this.trackingNumber = event.getTrackingNumber();
        this.shippedAt = event.getShippedAt();
        this.deliveredAt = event.getDeliveredAt();
        this.cancelledAt = event.getCancelledAt();
        this.lastEventId = event.getEventId();
        this.lastEventAt = event.getOccurredAt();
    }
}
