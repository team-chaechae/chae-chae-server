package com.project.inventoryservice.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "stock")
public class StockEntity {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public StockEntity(Long productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity != null ? quantity : 0;
    }

    public static StockEntity create(Long productId) {
        return new StockEntity(productId, 0);
    }

    public void increase(int amount) {
        this.quantity += amount;
    }

    public void decrease(int amount) {
        this.quantity -= amount;
    }
}
