package com.project.productservice.infrastructure;


import static com.project.productservice.domain.model.QProductEntity.productEntity;
import static org.springframework.util.StringUtils.hasText;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.ProductStatusType;
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
        Boolean includeDeleted, ProductStatusType productStatus ,LocalDate startDate, LocalDate endDate, LocalDate exactDate,
        List<String> sortList) {

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        List<ProductEntity> result = queryFactory.selectFrom(productEntity)
            .where(
                productNameLike(productName),
                deletedCondition(includeDeleted),
                productStatusEq(productStatus),
                dateRangeCondition(startDate, endDate, exactDate)
            ).orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(productEntity.count())
            .from(productEntity)
            .where(
                deletedCondition(includeDeleted),
                productNameLike(productName),
                productStatusEq(productStatus),
                dateRangeCondition(startDate, endDate, exactDate)
            );

        return PageableExecutionUtils.getPage(result, pageable, countQuery::fetchOne);
    }



    private BooleanExpression productNameLike(String productName) {
        return hasText(productName) ? productEntity.name.containsIgnoreCase(productName) : null;
    }


    private BooleanExpression productStatusEq(ProductStatusType status) {
        return status != null ? productEntity.productStatusType.eq(status) : null;
    }

    /**
     * 날짜 범위 조건 생성
     */
    private BooleanExpression dateRangeCondition(LocalDate startDate, LocalDate endDate, LocalDate exactDate) {
        // 특정 날짜만 조회
        if (exactDate != null) {
            LocalDateTime start = exactDate.atStartOfDay();
            LocalDateTime end = exactDate.plusDays(1).atStartOfDay();
            return productEntity.createdAt.goe(start).and(productEntity.createdAt.lt(end));
        }

        // 날짜 범위 지정
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            return productEntity.createdAt.goe(start).and(productEntity.createdAt.lt(end));
        } else if (startDate != null) {
            return productEntity.createdAt.goe(startDate.atStartOfDay());
        } else if (endDate != null) {
            LocalDateTime end = endDate.plusDays(1).atStartOfDay();
            return productEntity.createdAt.lt(end);
        }

        return null;
    }

    /**
     * 삭제 여부 조건 생성
     */
    private BooleanExpression deletedCondition(Boolean includeDeleted) {
        if (includeDeleted == null || !includeDeleted) {
            return productEntity.deletedAt.isNull();
        }
        return productEntity.deletedAt.isNotNull();
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
