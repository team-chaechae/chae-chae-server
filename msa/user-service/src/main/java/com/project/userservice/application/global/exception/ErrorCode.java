package com.project.userservice.application.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "USR-400-01", "잘못된 입력값입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USR-400-02", "비밀번호 형식이 올바르지 않습니다."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "USR-400-03", "이메일 형식이 올바르지 않습니다."),
    INVALID_EMPLOYEE_CODE(HttpStatus.BAD_REQUEST, "USR-400-04", "사원코드 형식이 올바르지 않습니다."),
    INVALID_POSITION(HttpStatus.BAD_REQUEST, "USR-400-05", "지원하지 않는 직책입니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "USR-401-01", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "USR-401-02", "유효하지 않은 토큰입니다."),

    // 403 Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "USR-403-01", "접근 권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-404-01", "존재하지 않는 사용자입니다."),

    // 409 Conflict
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR-409-01", "이미 존재하는 사용자입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR-409-02", "이미 존재하는 이메일입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "USR-500-01", "서버 내부 오류가 발생하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
