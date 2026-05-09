package com.project.chatbotservice.application.global.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException() {
        super("요청한 데이터를 찾을 수 없습니다.");
    }
}
