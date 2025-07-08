package com.project.chaechaeserver.domain.model.products;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Table(name = "products",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_name", columnNames = "name")
    })
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name ="category" , length = 50, nullable = false)
    private String category;

    @Column(name = "quantity")
    private Integer quantity = 0;

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    private List<InventoryEntity> inventoryHistories = new ArrayList<>();

    @Column(name = "price" ,nullable = false)
    private Integer price;

    @Column(name = "unit", length = 10, nullable = false)
    private String unit;


    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ProductEntity(String name, String category, Integer price, String unit, Integer initialQuantity) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.quantity = initialQuantity;
        this.inventoryHistories = new ArrayList<>();
    }

    public static ProductEntity createProducts(String name, String category, Integer price, String unit) {
        return ProductEntity.builder()
            .name(name)
            .category(category)
            .price(price)
            .unit(unit)
            .initialQuantity(null)
            .build();
    }

    public InventoryEntity addInventory(Integer quantity) {
        if (this.quantity == null) {
            this.quantity = quantity;
        } else {
            this.quantity += quantity;
        }

        InventoryEntity inventory = InventoryEntity.createInventory(this, quantity);
        this.inventoryHistories.add(inventory);
        return inventory;
    }

    public InventoryEntity decreaseInventory(Integer quantity) {
        if (this.quantity == null) {
            throw new IllegalStateException("재고 정보가 없습니다.");
        }
        this.quantity -= quantity;

        InventoryEntity inventory = InventoryEntity.createInventory(this, -quantity);
        this.inventoryHistories.add(inventory);
        return inventory;
    }


}
