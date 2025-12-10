package com.project.inventoryservice.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 생성 이벤트 DTO
 * Product Service에서 발행, Inventory Service에서 수신
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private Long productId;
    private String productName;
    private String category;
    private Integer price;
    private LocalDateTime createdAt;
}
