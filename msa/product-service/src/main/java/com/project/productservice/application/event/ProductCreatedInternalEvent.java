package com.project.productservice.application.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 생성 내부 이벤트
 * ApplicationEventPublisher를 통해 발행되는 Spring 내부 이벤트
 */
@Getter
@Builder
public class ProductCreatedInternalEvent {

    private final Long productId;
    private final String productName;
    private final String category;
    private final Integer price;
    private final LocalDateTime createdAt;

    public static ProductCreatedInternalEvent of(Long productId, String productName, String category, Integer price) {
        return ProductCreatedInternalEvent.builder()
                .productId(productId)
                .productName(productName)
                .category(category)
                .price(price)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
