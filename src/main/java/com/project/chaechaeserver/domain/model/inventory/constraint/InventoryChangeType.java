package com.project.chaechaeserver.domain.model.inventory.constraint;

import lombok.Getter;

@Getter
public enum InventoryChangeType {
    // 입출고 관리
    RECEIVE("입고"),
    SALE("판매"),
    ADJUST("재고 조정"),

    // 주문 관련
    ORDER_DECREASE("주문 차감"),
    ORDER_RESTORE("주문 복구"),

    // 시스템 동기화
    SYNC("시스템 동기화"),


    //히스토리 상태
    DLQ_FAILURE("히스토리 저장 실패");
    private final String displayName;

    InventoryChangeType(String displayName) {
        this.displayName = displayName;
    }
}
