package com.project.chaechaeserver.domain.model.order;

import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "orders")
public class OrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "order_id")
  private Long id;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  @Column(name = "unit_cost", nullable = false)
  private int unitCost;

  @Column(name = "total_cost", nullable = false)
  private int totalCost;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private StatusType status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;


  @Builder
  public OrderEntity(Long productId, int quantity, int unitCost, int totalCost, StatusType status) {
    this.productId = productId;
    this.quantity = quantity;
    this.unitCost = unitCost;
    this.totalCost = totalCost;
    this.status = status;
  }

  public static OrderEntity createOrder(Long productId, int quantity, int unitCost,
      StatusType status) {
    return OrderEntity.builder()
        .productId(productId)
        .quantity(quantity)
        .unitCost(unitCost)
        .totalCost(quantity * unitCost)
        .status(status)
        .build();
  }

}