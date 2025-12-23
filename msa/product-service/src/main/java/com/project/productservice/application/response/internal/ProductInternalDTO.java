package com.project.productservice.application.response.internal;

import com.project.productservice.domain.model.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInternalDTO {

    private Long productId;
    private String name;
    private String category;
    private Integer price;

    public static ProductInternalDTO from(ProductEntity product) {
        return ProductInternalDTO.builder()
            .productId(product.getId())
            .name(product.getName())
            .category(product.getCategory())
            .price(product.getPrice())
            .build();
    }
}
