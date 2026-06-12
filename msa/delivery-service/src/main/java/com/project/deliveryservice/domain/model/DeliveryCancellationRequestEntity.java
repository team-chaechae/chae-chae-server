package com.project.deliveryservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "delivery_cancellation_request",
        indexes = {
                @Index(name = "idx_delivery_cancel_request_sales_id", columnList = "sales_id", unique = true)
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryCancellationRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_id", nullable = false, unique = true)
    private Long salesId;

    @Column(name = "order_id", nullable = false, length = 80)
    private String orderId;

    @Column(length = 255)
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static DeliveryCancellationRequestEntity create(
            Long salesId,
            String orderId,
            String reason,
            LocalDateTime requestedAt
    ) {
        DeliveryCancellationRequestEntity request = new DeliveryCancellationRequestEntity();
        request.salesId = salesId;
        request.orderId = orderId;
        request.reason = reason;
        request.requestedAt = requestedAt;
        return request;
    }
}
