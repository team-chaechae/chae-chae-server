package com.project.chaechaeserver.infrastructure.product;


import static com.project.chaechaeserver.domain.model.products.QProductEntity.productEntity;
import static org.springframework.util.StringUtils.hasText;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
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
public class ProductQueryRepository
{
    private final JPAQueryFactory queryFactory;

    public Page<ProductEntity> findProductByDeletedAtIsNullWithCondition(Pageable pageable, String productName,
        Boolean deletedAt, ProductStatusType productStatus, ProductStatusType.ProductOrderType orderStatus ,LocalDate startDate, LocalDate endDate, LocalDate exactDate,
        List<String> sortList) {

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        List<ProductEntity> result = queryFactory.selectFrom(productEntity)
            .where(
                productNameLike(productName),
                isDeleted(deletedAt),
                productStatusEq(productStatus),
                orderStatusEq(orderStatus),
                createdAtCondition(startDate, endDate, exactDate)
            ).orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(productEntity.count())
            .from(productEntity)
            .where(
                isDeleted(deletedAt),
                productNameLike(productName),
                productStatusEq(productStatus),
                orderStatusEq(orderStatus),
                createdAtCondition(startDate, endDate, exactDate)
            );

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }


    private BooleanExpression createdAtCondition(LocalDate startDate, LocalDate endDate, LocalDate exactDate) {

        // 특정 날짜만 조회
        if (exactDate != null) {
            LocalDateTime start = exactDate.atStartOfDay();
            LocalDateTime end = exactDate.atTime(23, 59, 59);
            return productEntity.createdAt.between(start, end);
        }

        // 날짜 범위 지정에 따른 조회
        if (startDate != null && endDate != null) {
            return productEntity.createdAt.between(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        } else if (startDate != null) {
            return productEntity.createdAt.goe(startDate.atStartOfDay());
        } else if (endDate != null) {
            return productEntity.createdAt.loe(endDate.atTime(23, 59, 59));
        }

        return null;
    }

    private BooleanExpression productNameLike(String productName) {
        return hasText(productName) ? productEntity.name.containsIgnoreCase(productName) : null;
    }
    private BooleanExpression orderStatusEq(ProductStatusType.ProductOrderType orderStatus) {
        return orderStatus != null ? productEntity.orderStatusType.eq(orderStatus) : null;
    }
    private BooleanExpression isDeleted(Boolean deletedCond) {
        if (deletedCond != null) {
            return deletedCond ? productEntity.deletedAt.isNotNull() : productEntity.deletedAt.isNull();
        } else {
            return productEntity.deletedAt.isNull();
        }
    }

    private BooleanExpression productStatusEq(ProductStatusType status) {
        return status != null ? productEntity.productStatusType.eq(status) : null;
    }

    private List<OrderSpecifier<?>> buildOrderSpecifiers(List<String> sortOptions) {

        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (sortOptions != null) {
            for (String option : sortOptions) {
                switch (option) {
                    case "PRICE_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.price));
                    case "PRICE_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.price));
                    case "CREATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.createdAt));
                    case "CREATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.createdAt));
                    case "UPDATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.updatedAt));
                    case "UPDATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.updatedAt));
                    case "STATUS_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.productStatusType));
                    case "STATUS_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.productStatusType));
                    case "ORDER_STATUS_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, productEntity.orderStatusType));
                    case "ORDER_STATUS_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, productEntity.orderStatusType));

                    default -> {
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, productEntity.createdAt));
        }

        return orders;
    }


}
