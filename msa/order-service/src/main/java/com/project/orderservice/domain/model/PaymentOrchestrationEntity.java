package com.project.orderservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "payment_orchestrations",
        indexes = {
                @Index(name = "idx_payment_orchestration_sales_id", columnList = "sales_id", unique = true),
                @Index(name = "idx_payment_orchestration_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrchestrationEntity {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "sales_id", nullable = false)
    private Long salesId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentOrchestrationStatus status;

    @Column(name = "inventory_deducted", nullable = false)
    private boolean inventoryDeducted;

    @Column(name = "inventory_restored", nullable = false)
    private boolean inventoryRestored;

    @Column(name = "payment_refunded", nullable = false)
    private boolean paymentRefunded;

    @Column(name = "order_completed", nullable = false)
    private boolean orderCompleted;

    @Column(name = "order_cancelled", nullable = false)
    private boolean orderCancelled;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = ERROR_MESSAGE_MAX_LENGTH)
    private String lastError;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private PaymentOrchestrationEntity(String orderId, Long salesId) {
        this.orderId = orderId;
        this.salesId = salesId;
        this.status = PaymentOrchestrationStatus.STARTED;
    }

    public static PaymentOrchestrationEntity start(String orderId, Long salesId) {
        return new PaymentOrchestrationEntity(orderId, salesId);
    }

    public void markInventoryDeducted() {
        this.inventoryDeducted = true;
        this.status = PaymentOrchestrationStatus.INVENTORY_DEDUCTED;
        this.lastError = null;
    }

    public void markOrderCompleted() {
        this.orderCompleted = true;
        this.status = PaymentOrchestrationStatus.ORDER_COMPLETED;
        this.lastError = null;
    }

    public void startCompensation(String reason) {
        this.status = PaymentOrchestrationStatus.COMPENSATING;
        this.retryCount++;
        this.lastError = truncate(reason);
    }

    public void markInventoryRestored() {
        this.inventoryRestored = true;
        this.status = PaymentOrchestrationStatus.INVENTORY_RESTORED;
    }

    public void markPaymentRefunded() {
        this.paymentRefunded = true;
        this.status = PaymentOrchestrationStatus.PAYMENT_REFUNDED;
    }

    public void markOrderCancelled() {
        this.orderCancelled = true;
        this.status = PaymentOrchestrationStatus.ORDER_CANCELLED;
    }

    public void markFailed(String reason) {
        this.status = PaymentOrchestrationStatus.FAILED;
        this.retryCount++;
        this.lastError = truncate(reason);
    }

    public boolean isTerminal() {
        return orderCompleted || isCompensationCompleted() || status == PaymentOrchestrationStatus.FAILED;
    }

    public boolean isCompensating() {
        return status == PaymentOrchestrationStatus.COMPENSATING
                || status == PaymentOrchestrationStatus.INVENTORY_RESTORED
                || status == PaymentOrchestrationStatus.PAYMENT_REFUNDED
                || isPartialCompensation();
    }

    private boolean isCompensationCompleted() {
        return orderCancelled
                && paymentRefunded
                && (!inventoryDeducted || inventoryRestored);
    }

    private boolean isPartialCompensation() {
        return orderCancelled && !isCompensationCompleted();
    }

    private String truncate(String message) {
        if (message == null || message.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
