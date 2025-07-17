package com.project.chaechaeserver.domain.model.order;

import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private ProductEntity product;

  @Column(name = "unit_cost", nullable = false)
  private Integer unitCost;

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
  @Column(name = "created_by",updatable = false)
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
  public OrderEntity(ProductEntity product, Integer unitCost, Integer quantity, Integer totalCost, StatusType status) {
    this.product = product;
    this.unitCost = unitCost;
    this.quantity = quantity;
    this.totalCost = totalCost;
    this.status = status;
  }

  public static OrderEntity createOrder(ProductEntity product, Integer quantity, StatusType status) {
    int unitCost = product.getPrice();
    int totalCost = unitCost * quantity;
    return OrderEntity.builder()
        .product(product)
        .unitCost(unitCost)
        .quantity(quantity)
        .totalCost(totalCost)
        .status(status)
        .build();
  }

}