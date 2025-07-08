package com.project.chaechaeserver.domain.model.inventory;

import com.project.chaechaeserver.domain.model.products.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductEntity product;

    @Column(name = "quantity" , nullable = false)
    private Integer quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public InventoryEntity(ProductEntity product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public static InventoryEntity createInventory(ProductEntity product, Integer quantity) {
        return InventoryEntity.builder()
            .product(product)
            .quantity(quantity)
            .build();
    }
    public void updateInventory(ProductEntity products, Integer quantity) {
        this.product = products;
    }
}
