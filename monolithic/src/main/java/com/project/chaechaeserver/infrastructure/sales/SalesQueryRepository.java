package com.project.chaechaeserver.infrastructure.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
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

import static com.project.chaechaeserver.domain.model.sales.QSalesEntity.salesEntity;
import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class SalesQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<SalesEntity> findSalesByDeletedAtIsNullWithCondition(Pageable pageable, Boolean deletedCond, String productName,
                                                                     LocalDate startDate, LocalDate endDate, LocalDate exactDate,
                                                                     List<String> sortList) {

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        List<SalesEntity> results = queryFactory
                .selectFrom(salesEntity)
                .join(salesEntity.productEntity).fetchJoin()
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
                .select(salesEntity.count())
                .from(salesEntity)
                .where(
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
        return hasText(productName) ? salesEntity.productEntity.name.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression createdAtCondition(LocalDate startDate, LocalDate endDate, LocalDate exactDate) {

        // 특정 날짜만 조회
        if (exactDate != null) {
            LocalDateTime start = exactDate.atStartOfDay();
            LocalDateTime end = exactDate.atTime(23, 59, 59);
            return salesEntity.createdAt.between(start, end);
        }

        // 날짜 범위 지정에 따른 조회
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
                    case "PRICE_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, salesEntity.price));
                    case "PRICE_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, salesEntity.price));
                    case "CREATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, salesEntity.createdAt));
                    case "CREATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, salesEntity.createdAt));
                    default -> {
                    }
                }
            }
        }

        // 기본 정렬 (없을 경우 최신순)
        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, salesEntity.createdAt));
        }

        return orders;
    }
}
