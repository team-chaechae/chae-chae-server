package com.project.inventoryservice.domain.model.constraint;

import lombok.Getter;

@Getter
public enum InventoryStatus {
    RESERVED("예약"),
    CONFIRMED("확정"),
    CANCELLED("취소");

    private final String displayName;

    InventoryStatus(String displayName) {
        this.displayName = displayName;
    }
}
