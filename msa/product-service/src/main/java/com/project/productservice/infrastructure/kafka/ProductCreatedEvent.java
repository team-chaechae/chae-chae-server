package com.project.productservice.infrastructure.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 생성 이벤트
 * Product Service → Inventory Service
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

    public static ProductCreatedEvent of(Long productId, String productName, String category, Integer price) {
        return ProductCreatedEvent.builder()
                .productId(productId)
                .productName(productName)
                .category(category)
                .price(price)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
