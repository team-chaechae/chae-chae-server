package com.project.orderservice.domain.model;

import com.project.orderservice.domain.model.constraint.StatusType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "productId", column = @Column(name = "product_id")),
            @AttributeOverride(name = "productName", column = @Column(name = "product_name")),
            @AttributeOverride(name = "productCategory", column = @Column(name = "product_category")),
            @AttributeOverride(name = "productPrice", column = @Column(name = "product_price"))
    })
    private ProductInfo productInfo;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "total_cost", nullable = false)
    private Integer totalCost;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusType status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;


    @Builder
    public OrderEntity(ProductInfo productInfo, Integer quantity, Integer totalCost, StatusType status) {
        this.productInfo = productInfo;
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.status = status;
    }

    public static OrderEntity createOrder(ProductInfo productInfo, Integer quantity, StatusType status) {
        int totalCost = productInfo.getProductPrice() * quantity;
        return OrderEntity.builder()
                .productInfo(productInfo)
                .quantity(quantity)
                .totalCost(totalCost)
                .status(status)
                .build();
    }

    public void updateStatus(StatusType newStatus) {
        this.status = newStatus;
    }

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
        this.totalCost = newQuantity * this.productInfo.getProductPrice();
    }

}
