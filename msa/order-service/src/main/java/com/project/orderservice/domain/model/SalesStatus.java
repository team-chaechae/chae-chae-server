package com.project.orderservice.domain.model;

/**
 * 판매 주문 상태
 * Saga 패턴에서 주문 처리 상태를 추적
 */
public enum SalesStatus {
    PENDING,    // 주문 접수, 재고 차감 대기 중
    COMPLETED,  // 재고 차감 완료, 주문 성공
    CANCELLED   // 재고 부족 등으로 주문 실패
}
