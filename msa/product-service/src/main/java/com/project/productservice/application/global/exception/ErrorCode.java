package com.project.productservice.application.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "PRD-400-01", "잘못된 입력값입니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "PRD-400-02", "가격은 0보다 커야 합니다."),

    // 404 Not Found
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRD-404-01", "존재하지 않는 상품입니다."),
    SALES_NOT_FOUND(HttpStatus.NOT_FOUND, "PRD-404-02", "존재하지 않는 판매 기록입니다."),

    // 409 Conflict
    PRODUCT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PRD-409-01", "이미 존재하는 상품입니다."),
    PRODUCT_NAME_DUPLICATED(HttpStatus.CONFLICT, "PRD-409-02", "이미 존재하는 상품명입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PRD-500-01", "서버 내부 오류가 발생하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
