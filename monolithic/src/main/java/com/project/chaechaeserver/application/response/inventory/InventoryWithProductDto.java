package com.project.chaechaeserver.application.response.inventory;

import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.querydsl.core.annotations.QueryProjection;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * QueryDSL Projection용 DTO
 * Inventory와 Product 정보를 조인해서 가져올 때 사용
 */
@Getter
public class InventoryWithProductDto {

    private final Long inventoryId;
    private final Long productId;
    private final String productName;
    private final Integer productPrice;
    private final ProductStatusType productStatus;
    private final Integer quantity;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    @QueryProjection
    public InventoryWithProductDto(
        Long inventoryId,
        Long productId,
        String productName,
        Integer productPrice,
        ProductStatusType productStatus,
        Integer quantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.productName = productName;
        this.productPrice = productPrice;
        this.productStatus = productStatus;
        this.quantity = quantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}