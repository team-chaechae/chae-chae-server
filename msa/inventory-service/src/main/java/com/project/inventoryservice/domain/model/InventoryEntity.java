package com.project.inventoryservice.domain.model;

import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import com.project.inventoryservice.domain.model.constraint.InventoryStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_order_id", columnList = "order_id"),
    @Index(name = "idx_inventory_status", columnList = "status")
})
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private InventoryChangeType changeType;

    @Column(name = "order_id")
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private InventoryStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public InventoryEntity(Long productId, Integer quantity, InventoryChangeType changeType,
                           String orderId, InventoryStatus status) {
        this.productId = productId;
        this.quantity = quantity;
        this.changeType = changeType;
        this.orderId = orderId;
        this.status = status;
    }

    public static InventoryEntity createInventory(Long productId, Integer quantity, InventoryChangeType changeType) {
        return InventoryEntity.builder()
            .productId(productId)
            .quantity(quantity)
            .changeType(changeType)
            .build();
    }

    public static InventoryEntity createReserved(Long productId, Integer quantity, String orderId) {
        return InventoryEntity.builder()
            .productId(productId)
            .quantity(quantity)
            .changeType(InventoryChangeType.ORDER_DECREASE)
            .orderId(orderId)
            .status(InventoryStatus.RESERVED)
            .build();
    }

    public void confirm() {
        this.status = InventoryStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = InventoryStatus.CANCELLED;
    }
}
