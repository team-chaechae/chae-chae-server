package com.project.common.exception;

public class EntityAlreadyExistException extends RuntimeException {

    public EntityAlreadyExistException(String message) {
        super(message);
    }
}
