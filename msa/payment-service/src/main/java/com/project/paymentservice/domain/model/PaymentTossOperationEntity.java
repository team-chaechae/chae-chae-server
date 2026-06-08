package com.project.paymentservice.domain.model;

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
        name = "payment_toss_operations",
        indexes = {
                @Index(name = "idx_payment_toss_operation_id", columnList = "operation_id", unique = true),
                @Index(name = "idx_payment_toss_operation_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTossOperationEntity {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operation_id", nullable = false, length = 100)
    private String operationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private PaymentTossOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentTossOperationStatus status;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "sales_id", nullable = false)
    private Long salesId;

    @Column(name = "amount")
    private Integer amount;

    @Column(name = "toss_payment_key", nullable = false, length = 200)
    private String tossPaymentKey;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reason", length = 500)
    private String reason;

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

    private PaymentTossOperationEntity(
            String operationId,
            PaymentTossOperationType operationType,
            String orderId,
            Long salesId,
            Integer amount,
            String tossPaymentKey,
            String reason
    ) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.status = PaymentTossOperationStatus.STARTED;
        this.orderId = orderId;
        this.salesId = salesId;
        this.amount = amount;
        this.tossPaymentKey = tossPaymentKey;
        this.reason = reason;
    }

    public static PaymentTossOperationEntity confirm(
            String operationId,
            String orderId,
            Long salesId,
            Integer amount,
            String tossPaymentKey
    ) {
        return new PaymentTossOperationEntity(
                operationId,
                PaymentTossOperationType.CONFIRM,
                orderId,
                salesId,
                amount,
                tossPaymentKey,
                null
        );
    }

    public static PaymentTossOperationEntity cancel(
            String operationId,
            String orderId,
            Long salesId,
            String tossPaymentKey,
            String reason
    ) {
        return new PaymentTossOperationEntity(
                operationId,
                PaymentTossOperationType.CANCEL,
                orderId,
                salesId,
                null,
                tossPaymentKey,
                reason
        );
    }

    public void markConfirmTossSucceeded(String paymentMethod, LocalDateTime approvedAt) {
        this.status = PaymentTossOperationStatus.TOSS_SUCCEEDED;
        this.paymentMethod = paymentMethod;
        this.approvedAt = approvedAt;
        this.lastError = null;
    }

    public void markCancelTossSucceeded() {
        this.status = PaymentTossOperationStatus.TOSS_SUCCEEDED;
        this.lastError = null;
    }

    public void markLocalRecorded() {
        this.status = PaymentTossOperationStatus.LOCAL_RECORDED;
        this.lastError = null;
    }

    public void markLocalRecordFailed(String reason) {
        this.status = PaymentTossOperationStatus.TOSS_SUCCEEDED;
        this.retryCount++;
        this.lastError = truncate(reason);
    }

    public void markFailed(String reason) {
        this.status = PaymentTossOperationStatus.FAILED;
        this.retryCount++;
        this.lastError = truncate(reason);
    }

    private String truncate(String message) {
        if (message == null || message.length() <= ERROR_MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}
