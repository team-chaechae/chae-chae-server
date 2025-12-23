package com.project.chatbotservice.application.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "CHT-400-01", "잘못된 입력값입니다."),
    INVALID_QUESTION(HttpStatus.BAD_REQUEST, "CHT-400-02", "질문 형식이 올바르지 않습니다."),
    EMPTY_MESSAGE(HttpStatus.BAD_REQUEST, "CHT-400-03", "메시지가 비어있습니다."),

    // 404 Not Found
    CHATBOT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHT-404-01", "존재하지 않는 챗봇입니다."),
    DATASET_NOT_FOUND(HttpStatus.NOT_FOUND, "CHT-404-02", "존재하지 않는 데이터셋입니다."),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CHT-404-03", "존재하지 않는 대화입니다."),

    // 409 Conflict
    DATASET_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHT-409-01", "이미 존재하는 데이터셋입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CHT-500-01", "서버 내부 오류가 발생하였습니다."),
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CHT-500-02", "AI 서비스 연동 중 오류가 발생하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
