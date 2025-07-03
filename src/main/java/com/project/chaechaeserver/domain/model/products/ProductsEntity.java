package com.project.chaechaeserver.domain.model.products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Table(name = "products")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProductsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String category;

    @Column(nullable = false)
    private int price;

    @Column(length = 10, nullable = false)
    private String unit;

    @Column(nullable = false)
    private boolean isDeleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public ProductsEntity(String name, String category, int price, String unit, boolean isDeleted) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.unit = unit;
        this.isDeleted = isDeleted;
    }

    public static ProductsEntity createProducts(String name, String category, int price, String unit, boolean isDeleted) {
        return ProductsEntity.builder()
            .name(name)
            .category(category)
            .price(price)
            .unit(unit)
            .isDeleted(isDeleted)
            .build();
    }


}
