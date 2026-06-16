package com.project.inventoryservice.application.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INV-400-01", "잘못된 입력값입니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INV-400-02", "재고 수량은 0보다 커야 합니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "INV-409-02", "재고가 부족합니다."),

    // 404 Not Found
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "INV-404-01", "존재하지 않는 재고입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "INV-404-02", "존재하지 않는 상품입니다."),

    // 409 Conflict
    INVENTORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "INV-409-01", "이미 존재하는 재고입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INV-500-01", "서버 내부 오류가 발생하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
