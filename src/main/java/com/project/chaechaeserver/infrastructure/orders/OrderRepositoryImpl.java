package com.project.chaechaeserver.infrastructure.orders;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.QOrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<OrderEntity> searchOrdersByFilter(Pageable pageable, Long orderId, String productName,
      String productCategory, StatusType status, String createdBy , LocalDate startDate, LocalDate endDate, List<String> sortList) {

    QOrderEntity order = QOrderEntity.orderEntity;

    BooleanBuilder builder = new BooleanBuilder();

    if (orderId != null) {
      builder.and(order.id.eq(orderId));
    }

    if (productName != null) {
      builder.and(order.productInfo.productName.contains(productName));
    }

    if (productCategory != null) {
        builder.and(order.productInfo.productCategory.eq(productCategory));
    }

    if (status != null) {
      builder.and(order.status.eq(status));
    }

    if (createdBy != null) {
      builder.and(order.createdBy.eq(createdBy));
    }

    if (startDate != null) {
      builder.and(order.createdAt.goe(startDate.atStartOfDay()));
    }

    if (endDate != null) {
      builder.and(order.createdAt.loe(endDate.atTime(LocalTime.MAX)));
    }

    // 정렬 처리
    List<OrderSpecifier<?>> orders = buildOrderSpecifiers(sortList, order);

    // 결과 목록
    List<OrderEntity> results = queryFactory
        .selectFrom(order)
        .where(builder)
        .orderBy(orders.toArray(new OrderSpecifier[0]))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    // 총 개수
    long total = queryFactory
        .selectFrom(order)
        .where(builder)
        .fetchCount();

    return new PageImpl<>(results, pageable, total);
  }

  private List<OrderSpecifier<?>> buildOrderSpecifiers(List<String> sortList, QOrderEntity order) {
    List<OrderSpecifier<?>> orders = new ArrayList<>();

    if (sortList != null) {
      for (String sort : sortList) {
        switch (sort) {
          case "CREATED_AT_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, order.createdAt));
          case "CREATED_AT_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, order.createdAt));
          case "TOTAL_COST_DESC" -> orders.add(new OrderSpecifier<>(Order.DESC, order.totalCost));
          case "TOTAL_COST_ASC" -> orders.add(new OrderSpecifier<>(Order.ASC, order.totalCost));
        }
      }
    }

    // 정렬이 없다면 기본 정렬
    if (orders.isEmpty()) {
      orders.add(new OrderSpecifier<>(Order.DESC, order.createdAt));
    }

    return orders;
  }
}
