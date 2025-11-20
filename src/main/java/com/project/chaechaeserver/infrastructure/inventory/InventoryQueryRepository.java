package com.project.chaechaeserver.infrastructure.inventory;

import static com.project.chaechaeserver.domain.model.inventory.QInventoryEntity.inventoryEntity;
import static com.project.chaechaeserver.domain.model.products.QProductEntity.productEntity;
import static org.springframework.util.StringUtils.hasText;

import com.project.chaechaeserver.application.response.inventory.InventoryWithProductDto;
import com.project.chaechaeserver.application.response.inventory.QInventoryWithProductDto;
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
        Boolean includeDeleted,
        ProductStatusType productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList) {

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        List<InventoryEntity> result = queryFactory
            .selectFrom(inventoryEntity)
            .leftJoin(productEntity).on(inventoryEntity.productId.eq(productEntity.id))
            .where(
                productNameLike(productName),
                productStatusEq(productStatus),
                productIdEq(productId),
                dateRangeCondition(startDate, endDate, exactDate),
                deletedCondition(includeDeleted)
            )
            .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(inventoryEntity.count())
            .from(inventoryEntity)
            .leftJoin(productEntity).on(inventoryEntity.productId.eq(productEntity.id))
            .where(
                productNameLike(productName),
                productStatusEq(productStatus),
                productIdEq(productId),
                dateRangeCondition(startDate, endDate, exactDate),
                deletedCondition(includeDeleted)
            );

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }


    private BooleanExpression productNameLike(String productName) {
        return hasText(productName) ? productEntity.name.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression productStatusEq(ProductStatusType status) {
        return status != null ? productEntity.productStatusType.eq(status) : null;
    }

    private BooleanExpression productIdEq(Long productId) {
        return productId != null ? inventoryEntity.productId.eq(productId) : null;
    }

    /**
     * 날짜 범위 조건 생성
     */
    private BooleanExpression dateRangeCondition(LocalDate startDate, LocalDate endDate, LocalDate exactDate) {
        // 특정 날짜만 조회
        if (exactDate != null) {
            LocalDateTime start = exactDate.atStartOfDay();
            LocalDateTime end = exactDate.plusDays(1).atStartOfDay();
            return inventoryEntity.createdAt.goe(start).and(inventoryEntity.createdAt.lt(end));
        }

        // 날짜 범위 지정
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            return inventoryEntity.createdAt.goe(start).and(inventoryEntity.createdAt.lt(end));
        } else if (startDate != null) {
            return inventoryEntity.createdAt.goe(startDate.atStartOfDay());
        } else if (endDate != null) {
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            return inventoryEntity.createdAt.lt(end);
        }

        return null;
    }

    /**
     * 삭제 여부 조건 생성
     */
    private BooleanExpression deletedCondition(Boolean includeDeleted) {
        if (includeDeleted == null || !includeDeleted) {
            return inventoryEntity.deletedAt.isNull();
        }
        return inventoryEntity.deletedAt.isNotNull();
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

    /**
     * 상품의 현재 재고 조회 (히스토리 합계)
     * @param productId 상품 ID
     * @return 현재 재고 (히스토리가 없으면 0)
     */
    public Integer getCurrentStock(Long productId) {
        Integer stock = queryFactory
            .select(inventoryEntity.quantity.sum())
            .from(inventoryEntity)
            .where(
                inventoryEntity.productId.eq(productId),
                inventoryEntity.deletedAt.isNull()
            )
            .fetchOne();

        return stock != null ? stock : 0;
    }

    /**
     * 여러 상품의 현재 재고를 한 번에 조회
     * @param productIds 상품 ID 목록
     * @return productId -> 현재 재고 맵
     */
    public java.util.Map<Long, Integer> getCurrentStockMap(List<Long> productIds) {
        List<com.querydsl.core.Tuple> results = queryFactory
            .select(
                inventoryEntity.productId,
                inventoryEntity.quantity.sum()
            )
            .from(inventoryEntity)
            .where(
                inventoryEntity.productId.in(productIds),
                inventoryEntity.deletedAt.isNull()
            )
            .groupBy(inventoryEntity.productId)
            .fetch();

        java.util.Map<Long, Integer> stockMap = new java.util.HashMap<>();

        // 조회된 결과 매핑
        for (com.querydsl.core.Tuple tuple : results) {
            Long productId = tuple.get(inventoryEntity.productId);
            Integer stock = tuple.get(inventoryEntity.quantity.sum());
            stockMap.put(productId, stock != null ? stock : 0);
        }

        // 조회되지 않은 상품은 0으로 초기화
        for (Long productId : productIds) {
            stockMap.putIfAbsent(productId, 0);
        }

        return stockMap;
    }

    /**
     * Inventory와 Product 정보를 조인해서 조회 (Projection 사용)
     * Product 정보를 포함한 DTO로 반환
     */
    public Page<InventoryWithProductDto> findInventoryWithProduct(
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

        // Projection을 사용해서 Product 정보를 포함한 DTO로 조회
        List<InventoryWithProductDto> result = queryFactory
            .select(new QInventoryWithProductDto(
                inventoryEntity.id,
                inventoryEntity.productId,
                productEntity.name,
                productEntity.price,
                productEntity.productStatusType,
                inventoryEntity.quantity,
                inventoryEntity.createdAt,
                inventoryEntity.updatedAt
            ))
            .from(inventoryEntity)
            .leftJoin(productEntity).on(inventoryEntity.productId.eq(productEntity.id))
            .where(
                productNameLike(productName),
                productStatusEq(productStatus),
                productIdEq(productId),
                dateRangeCondition(startDate, endDate, exactDate),
                deletedCondition(deletedAt)
            )
            .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        // 카운트 쿼리 - Product 정보는 항상 필요하므로 조인
        JPAQuery<Long> countQuery = queryFactory
            .select(inventoryEntity.count())
            .from(inventoryEntity)
            .leftJoin(productEntity).on(inventoryEntity.productId.eq(productEntity.id))
            .where(
                productNameLike(productName),
                productStatusEq(productStatus),
                productIdEq(productId),
                dateRangeCondition(startDate, endDate, exactDate),
                deletedCondition(deletedAt)
            );

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }
}