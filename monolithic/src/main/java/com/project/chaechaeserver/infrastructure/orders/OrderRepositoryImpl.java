package com.project.chaechaeserver.infrastructure.orders;

import static com.project.chaechaeserver.domain.model.order.QOrderEntity.orderEntity;
import static io.jsonwebtoken.lang.Strings.hasText;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<OrderEntity> searchOrdersByFilter(Pageable pageable, Long orderId, String productName,
                                                  String productCategory, StatusType status, String createdBy,
                                                  LocalDate startDate, LocalDate endDate, List<String> sortList) {

        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortList);

        // 결과 목록
        List<OrderEntity> results = queryFactory
                .selectFrom(orderEntity)
                .where(
                        eqOrderId(orderId),
                        containsProductName(productName),
                        eqProductCategory(productCategory),
                        eqStatus(status),
                        eqCreatedBy(createdBy),
                        betweenCreatedAt(startDate, endDate)
                )
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수
        JPAQuery<Long> countQuery = queryFactory
                .select(orderEntity.count())
                .from(orderEntity)
                .where(
                        eqOrderId(orderId),
                        containsProductName(productName),
                        eqProductCategory(productCategory),
                        eqStatus(status),
                        eqCreatedBy(createdBy),
                        betweenCreatedAt(startDate, endDate)
                );

        return PageableExecutionUtils.getPage(results, pageable, countQuery::fetchOne);
    }

    private BooleanExpression eqOrderId(Long orderId) {
        return orderId != null ? orderEntity.id.eq(orderId) : null;
    }

    private BooleanExpression containsProductName(String productName) {
        return hasText(productName) ? orderEntity.productInfo.productName.containsIgnoreCase(productName) : null;
    }

    private BooleanExpression eqProductCategory(String productCategory) {
        return productCategory != null ? orderEntity.productInfo.productCategory.eq(productCategory) : null;
    }

    private BooleanExpression eqStatus(StatusType status) {
        return status != null ? orderEntity.status.eq(status) : null;
    }

    private BooleanExpression eqCreatedBy(String createdBy) {
        return hasText(createdBy) ? orderEntity.createdBy.eq(createdBy) : null;
    }

    private BooleanExpression betweenCreatedAt(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return orderEntity.createdAt.between(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
        } else if (startDate != null) {
            return orderEntity.createdAt.goe(startDate.atStartOfDay());
        } else if (endDate != null) {
            return orderEntity.createdAt.loe(endDate.atTime(LocalTime.MAX));
        }
        return null;
    }


    // 정렬 조건
    private List<OrderSpecifier<?>> buildOrderSpecifiers(List<String> sortList) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (sortList != null) {
            for (String sort : sortList) {
                switch (sort) {
                    case "CREATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, orderEntity.createdAt));
                    case "CREATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, orderEntity.createdAt));
                    case "UPDATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, orderEntity.updatedAt));
                    case "UPDATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, orderEntity.updatedAt));
                    default -> {
                    }
                }
            }
        }

        // 정렬이 없다면 기본 정렬
        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, orderEntity.createdAt));
        }

        return orders;
    }
}
