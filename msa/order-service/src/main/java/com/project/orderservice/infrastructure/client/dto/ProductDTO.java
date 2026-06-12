package com.project.orderservice.infrastructure.client.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long productId;
    private String name;
    private String category;
    private Integer price;
    private Integer originalPrice;
    private boolean promotionApplied;
    private String promotionType;
    private Integer promotionPrice;
    private LocalDateTime promotionEndsAt;
}
