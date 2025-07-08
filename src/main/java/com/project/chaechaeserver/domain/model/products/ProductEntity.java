package com.project.chaechaeserver.domain.model.products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
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

    @Column(name = "price" ,nullable = false)
    private int price;

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
    public ProductEntity(String name, String category, int price, String unit) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.unit = unit;
    }

    public static ProductEntity createProducts(String name, String category, int price, String unit ) {
        return ProductEntity.builder()
            .name(name)
            .category(category)
            .price(price)
            .unit(unit)
            .build();
    }


}
