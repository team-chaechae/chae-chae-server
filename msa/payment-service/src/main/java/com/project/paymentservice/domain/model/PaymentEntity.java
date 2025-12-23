package com.project.paymentservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payment")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "sales_id", nullable = false, unique = true)
    private Long salesId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentHistoryEntity> histories = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private PaymentEntity(String orderId, Long salesId, Integer amount) {
        this.orderId = orderId;
        this.salesId = salesId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public static PaymentEntity create(String orderId, Long salesId, Integer amount) {
        PaymentEntity payment = PaymentEntity.builder()
                .orderId(orderId)
                .salesId(salesId)
                .amount(amount)
                .build();
        payment.addHistory(PaymentStatus.PENDING, "결제 요청 생성");
        return payment;
    }

    public void process() {
        this.status = PaymentStatus.PROCESSING;
        addHistory(PaymentStatus.PROCESSING, "결제 처리 시작");
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
        addHistory(PaymentStatus.COMPLETED, "결제 완료");
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        addHistory(PaymentStatus.FAILED, reason);
    }

    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.failureReason = reason;
        addHistory(PaymentStatus.CANCELLED, reason);
    }

    public void refund(String reason) {
        this.status = PaymentStatus.REFUNDED;
        addHistory(PaymentStatus.REFUNDED, reason);
    }

    private void addHistory(PaymentStatus status, String description) {
        PaymentHistoryEntity history = PaymentHistoryEntity.create(this, status, description);
        this.histories.add(history);
    }
}
