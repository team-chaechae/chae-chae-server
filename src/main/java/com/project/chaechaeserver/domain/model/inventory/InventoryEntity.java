package com.project.chaechaeserver.domain.model.inventory;

import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Table(name = "inventory")
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

    @Column(name = "quantity" , nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private InventoryChangeType changeType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public InventoryEntity(Long productId, Integer quantity, InventoryChangeType changeType) {
        this.productId = productId;
        this.quantity = quantity;
        this.changeType = changeType;
    }

    public static InventoryEntity createInventory(Long productId, Integer quantity, InventoryChangeType changeType) {
        return InventoryEntity.builder()
            .productId(productId)
            .quantity(quantity)
            .changeType(changeType)
            .build();
    }

}
