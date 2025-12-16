package com.project.orderservice.domain.model;

/**
 * 판매 주문 상태
 * Saga 패턴에서 주문 처리 상태를 추적
 */
public enum SalesStatus {
    PENDING,     // 주문 접수, Saga 시작 대기 중
    PROCESSING,  // Saga 진행 중 (재고 예약/결제 처리 중)
    COMPLETED,   // Saga 완료, 주문 성공
    CANCELLED    // Saga 실패, 주문 취소
}
