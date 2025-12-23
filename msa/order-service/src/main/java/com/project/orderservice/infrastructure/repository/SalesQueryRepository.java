package com.project.orderservice.infrastructure.repository;

import com.project.orderservice.domain.model.SalesEntity;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.project.orderservice.domain.model.QSalesEntity.salesEntity;
import static com.project.orderservice.domain.model.QSalesItemEntity.salesItemEntity;
import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class SalesQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<SalesEntity> findSalesByDeletedAtIsNullWithCondition(
            Pageable pageable,
            Boolean deletedCond,
            String productName,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate exactDate,
            List<String> sortList
    ) {
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        // productName 검색 시 SalesItem과 조인 필요
        JPAQuery<SalesEntity> query = queryFactory
                .selectFrom(salesEntity)
                .distinct();

        if (hasText(productName)) {
            query.leftJoin(salesEntity.items, salesItemEntity);
        }

        List<SalesEntity> results = query
                .where(
                        isDeleted(deletedCond),
                        productNameLike(productName),
                        createdAtCondition(startDate, endDate, exactDate)
                )
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(salesEntity.countDistinct())
                .from(salesEntity);

        if (hasText(productName)) {
            countQuery.leftJoin(salesEntity.items, salesItemEntity);
        }

        countQuery.where(
                isDeleted(deletedCond),
                productNameLike(productName),
                createdAtCondition(startDate, endDate, exactDate)
        );

        return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchOne);
    }

    private BooleanExpression isDeleted(Boolean deletedCond) {
        if (deletedCond != null) {
            return deletedCond ? salesEntity.deletedAt.isNotNull() : salesEntity.deletedAt.isNull();
        } else {
            return salesEntity.deletedAt.isNull();
        }
    }

    private BooleanExpression productNameLike(String productName) {
        return hasText(productName) ? salesItemEntity.productName.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression createdAtCondition(LocalDate startDate, LocalDate endDate, LocalDate exactDate) {
        if (exactDate != null) {
            LocalDateTime start = exactDate.atStartOfDay();
            LocalDateTime end = exactDate.atTime(23, 59, 59);
            return salesEntity.createdAt.between(start, end);
        }

        if (startDate != null && endDate != null) {
            return salesEntity.createdAt.between(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        } else if (startDate != null) {
            return salesEntity.createdAt.goe(startDate.atStartOfDay());
        } else if (endDate != null) {
            return salesEntity.createdAt.loe(endDate.atTime(23, 59, 59));
        }

        return null;
    }

    private List<OrderSpecifier<?>> buildOrderSpecifiers(List<String> sortOptions) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (sortOptions != null) {
            for (String option : sortOptions) {
                switch (option) {
                    case "CREATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, salesEntity.createdAt));
                    case "CREATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, salesEntity.createdAt));
                    case "STATUS_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, salesEntity.status));
                    case "STATUS_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, salesEntity.status));
                    default -> {
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, salesEntity.createdAt));
        }

        return orders;
    }
}
