package com.project.orderservice.application.global.exception;

import com.project.common.dlq.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "ORD-400-01", "잘못된 입력값입니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORD-400-02", "수량은 1개 이상이어야 합니다."),
    INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "ORD-400-03", "상태 변경이 불가능합니다."),
    ONLY_APPROVED_STATUS_CAN_CHANGE(HttpStatus.BAD_REQUEST, "ORD-400-04", "승인 상태에서만 상태를 변경할 수 있습니다."),
    ONLY_APPROVED_STATUS_CAN_UPDATE_QUANTITY(HttpStatus.BAD_REQUEST, "ORD-400-05", "승인 상태에서만 수량을 변경할 수 있습니다."),

    // 404 Not Found
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD-404-01", "존재하지 않는 발주입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "ORD-404-02", "존재하지 않는 상품입니다."),

    // 409 Conflict
    ORDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "ORD-409-01", "이미 발주 중인 상품입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ORD-500-01", "서버 내부 오류가 발생하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
