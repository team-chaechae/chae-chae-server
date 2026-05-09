package com.project.productservice.domain.model.constraint;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProductStatusType {

    AVAILABLE(Status.AVAILABLE, "판매중"),
    PENDING(Status.PENDING, "판매대기"),
    OUT_OF_STOCK(Status.OUT_OF_STOCK, "재고없음");

    private final String status;
    private final String displayName;

    public static class Status {
        public static final String AVAILABLE = "STATUS_AVAILABLE";
        public static final String PENDING = "STATUS_PENDING";
        public static final String OUT_OF_STOCK = "STATUS_OUT_OF_STOCK";
    }

}
