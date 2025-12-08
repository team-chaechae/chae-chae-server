package com.project.orderservice.application.global.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return new ResponseEntity<>(
                ErrorResponse.of(errorCode, e.getMessage()),
                errorCode.getHttpStatus()
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException e) {
        log.error("EntityNotFoundException: {}", e.getMessage());
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.ORDER_NOT_FOUND, e.getMessage()),
                ErrorCode.ORDER_NOT_FOUND.getHttpStatus()
        );
    }

    @ExceptionHandler(EntityAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleEntityAlreadyExistException(EntityAlreadyExistException e) {
        log.error("EntityAlreadyExistException: {}", e.getMessage());
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.ORDER_ALREADY_EXISTS, e.getMessage()),
                ErrorCode.ORDER_ALREADY_EXISTS.getHttpStatus()
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        log.error("BadRequestException: {}", e.getMessage());
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getMessage()),
                ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());
        StringBuilder errorMessage = new StringBuilder();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMessage.append(fieldName).append(": ").append(message).append("; ");
        });
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, errorMessage.toString().trim()),
                ErrorCode.INVALID_INPUT_VALUE.getHttpStatus()
        );
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        log.error("FeignException: status={}, message={}", e.status(), e.getMessage());

        HttpStatus status;
        String message;

        switch (e.status()) {
            case 404 -> {
                status = HttpStatus.NOT_FOUND;
                message = "요청한 리소스를 찾을 수 없습니다.";
            }
            case 400 -> {
                status = HttpStatus.BAD_REQUEST;
                message = "잘못된 요청입니다.";
            }
            case 503, -1 -> {
                status = HttpStatus.SERVICE_UNAVAILABLE;
                message = "서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.";
            }
            default -> {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                message = "외부 서비스 호출 중 오류가 발생했습니다.";
            }
        }

        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, message),
                status
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage(), e);
        return new ResponseEntity<>(
                ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR),
                ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus()
        );
    }
}
