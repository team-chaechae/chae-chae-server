package com.project.chaechaeserver.domain.model.order;

import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "order_customer")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OrderCustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", length = 20)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @Column(name = "total_price")
    private Integer totalPrice;

    @OneToMany(mappedBy = "orderCustomer" , cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

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

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder
    public OrderCustomerEntity(Long customerId, Integer totalQuantity, Integer totalPrice, StatusType status, LocalDateTime expiresAt) {
        this.customerId = customerId;
        this.totalQuantity = totalQuantity;
        this.totalPrice = totalPrice;
        this.status = status;
        this.expiresAt = expiresAt;
    }


    public static OrderCustomerEntity createOrder(ReqOrderCustomerPostCreateDTO dto) {
        List<OrderItemEntity> orderItems = dto.getOrder().getOrderItems().stream()
            .map(item -> OrderItemEntity.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build())
            .collect(Collectors.toList());

        int totalQuantity = 0;
        int totalPrice = 0;

        for (OrderItemEntity item : orderItems) {
            totalQuantity += item.getQuantity();
            totalPrice += item.getPrice() * item.getQuantity();
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(300);

        OrderCustomerEntity order = OrderCustomerEntity.builder()
            .customerId(dto.getOrder().getCustomerId())
            .status(StatusType.PENDING)
            .totalQuantity(totalQuantity)
            .totalPrice(totalPrice)
            .expiresAt(expiresAt)
            .build();

        order.addOrderItems(orderItems);

        return order;
    }



    public void addOrderItem(OrderItemEntity orderItem) {
        this.orderItems.add(orderItem);
        orderItem.belongsTo(this);
    }

    public void addOrderItems(List<OrderItemEntity> orderItems) {
        orderItems.forEach(this::addOrderItem);
    }

    public void updateStatus(StatusType newStatus) {
        this.status = newStatus;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void expire() {
        this.status = StatusType.CANCELLED;
    }

}
