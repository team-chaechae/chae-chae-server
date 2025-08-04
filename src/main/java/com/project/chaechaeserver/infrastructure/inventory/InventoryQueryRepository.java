package com.project.chaechaeserver.infrastructure.inventory;

import static com.project.chaechaeserver.domain.model.inventory.QInventoryEntity.inventoryEntity;
import static com.project.chaechaeserver.domain.model.products.QProductEntity.productEntity;
import static org.springframework.util.StringUtils.hasText;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<InventoryEntity> findInventoryWithCondition(
        Pageable pageable,
        String productName,
        Boolean deletedAt,
        ProductStatusType productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList) {

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        List<InventoryEntity> result = queryFactory
            .selectFrom(inventoryEntity)
            .leftJoin(inventoryEntity.product, productEntity).fetchJoin()
            .where(
                productNameLike(productName),
                productStatusEq(productStatus),
                productIdEq(productId),
                createdAtCondition(startDate, endDate, exactDate),
                isdDeletedAt(deletedAt)
            )
            .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(inventoryEntity.count())
            .from(inventoryEntity)
            .leftJoin(inventoryEntity.product, productEntity)
            .where(
                productNameLike(productName),
                productStatusEq(productStatus),
                productIdEq(productId),
                createdAtCondition(startDate, endDate, exactDate),
                isdDeletedAt(deletedAt)
            );

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }

    private BooleanExpression createdAtCondition(LocalDate startDate, LocalDate endDate, LocalDate exactDate) {
        // 특정 날짜만 조회
        if (exactDate != null) {
            LocalDateTime start = exactDate.atStartOfDay();
            LocalDateTime end = exactDate.atTime(23, 59, 59);
            return inventoryEntity.createdAt.between(start, end);
        }

        // 날짜 범위 지정에 따른 조회
        if (startDate != null && endDate != null) {
            return inventoryEntity.createdAt.between(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        } else if (startDate != null) {
            return inventoryEntity.createdAt.goe(startDate.atStartOfDay());
        } else if (endDate != null) {
            return inventoryEntity.createdAt.loe(endDate.atTime(23, 59, 59));
        }

        return null;
    }

    private BooleanExpression productNameLike(String productName) {
        return hasText(productName) ? productEntity.name.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression productStatusEq(ProductStatusType status) {
        return status != null ? productEntity.productStatusType.eq(status) : null;
    }

    private BooleanExpression productIdEq(Long productId) {
        return productId != null ? inventoryEntity.product.id.eq(productId) : null;
    }

    private BooleanExpression isdDeletedAt(Boolean deletedCond) {
        if (deletedCond != null) {
            return deletedCond ? inventoryEntity.deletedAt.isNotNull() : inventoryEntity.deletedAt.isNull();
        } else {
            return inventoryEntity.deletedAt.isNull();
        }
    }

    private List<OrderSpecifier<?>> buildOrderSpecifiers(List<String> sortOptions) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (sortOptions != null) {
            for (String option : sortOptions) {
                switch (option) {
                    case "PRODUCT_NAME_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.name));
                    case "PRODUCT_NAME_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.name));
                    case "PRODUCT_PRICE_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.price));
                    case "PRODUCT_PRICE_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.price));
                    case "CREATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, inventoryEntity.createdAt));
                    case "CREATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, inventoryEntity.createdAt));
                    case "UPDATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, inventoryEntity.updatedAt));
                    case "UPDATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, inventoryEntity.updatedAt));
                    default -> {
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, inventoryEntity.createdAt));
        }

        return orders;
    }
}