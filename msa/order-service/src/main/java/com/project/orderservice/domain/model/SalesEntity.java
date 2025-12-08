package com.project.orderservice.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sales")
public class SalesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_id")
    private Long id;

    @OneToMany(mappedBy = "sales", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalesItemEntity> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    protected SalesEntity() {
        this.status = SalesStatus.PENDING;
    }

    /**
     * 정적 팩토리 메서드 - Sales와 Items를 함께 생성
     */
    public static SalesEntity createWithItems(List<SalesItemEntity> items) {
        SalesEntity sales = new SalesEntity();
        items.forEach(sales::addItem);
        return sales;
    }

    /**
     * 양방향 연관관계 편의 메서드
     */
    private void addItem(SalesItemEntity item) {
        items.add(item);
        item.assignSales(this);
    }

    public void complete() {
        this.status = SalesStatus.COMPLETED;
    }

    public void cancel(String reason) {
        this.status = SalesStatus.CANCELLED;
        this.failureReason = reason;
    }

    public int getTotalPrice() {
        return items.stream()
            .mapToInt(SalesItemEntity::getTotalPrice)
            .sum();
    }

    public int getTotalQuantity() {
        return items.stream()
            .mapToInt(SalesItemEntity::getQuantity)
            .sum();
    }
}
