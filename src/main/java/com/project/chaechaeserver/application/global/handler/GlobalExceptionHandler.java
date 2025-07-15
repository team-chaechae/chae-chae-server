package com.project.chaechaeserver.application.global.handler;

import com.project.chaechaeserver.application.global.constants.ResCode;
import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.global.excepion.*;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===============================================================
    // ✅ 1. 요청/검증 관련 예외
    // ===============================================================

    /**
     * RequestBody 또는 필드의 유효성 검사 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResDTO<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("유효성 검사 실패");

        return ResponseEntity.badRequest().body(
                new ResDTO<>(ResCode.BAD_REQUEST_EXCEPTION, errorMessage, null)
        );
    }

    /**
     * PathVariable / QueryParam 타입이 잘못된 경우
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResDTO<Object>> handleMethodArgumentTypeMismatchException(Exception e) {
        return ResponseEntity.badRequest().body(
                ResDTO.builder()
                        .code(ResCode.METHOD_ARGUMENT_TYPE_MISMATCH_EXCEPTION)
                        .message("PathVariable 또는 QueryString 타입을 확인하세요.")
                        .build()
        );
    }

    /**
     * RequestBody가 없거나 JSON 파싱이 실패한 경우
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResDTO<Object>> handleHttpMessageNotReadableException(Exception e) {
        if (e.getMessage().contains("Required request body is missing")) {
            return ResponseEntity.badRequest().body(
                    ResDTO.builder()
                            .code(ResCode.HTTP_MESSAGE_NOT_READABLE_EXCEPTION)
                            .message("RequestBody가 없습니다.")
                            .build()
            );
        }
        if (e.getMessage().contains("Enum class: ")) {
            return ResponseEntity.badRequest().body(
                    ResDTO.builder()
                            .code(ResCode.HTTP_MESSAGE_NOT_READABLE_EXCEPTION)
                            .message("Enum 타입 매개변수를 확인하세요.")
                            .build()
            );
        }
        return ResponseEntity.badRequest().body(
                ResDTO.builder()
                        .code(ResCode.HTTP_MESSAGE_NOT_READABLE_EXCEPTION)
                        .message("RequestBody를 형식에 맞추어 주세요.")
                        .build()
        );
    }

    /**
     * 커스텀 BadRequest 예외 처리
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResDTO<Object>> handleBadRequestException(Exception e) {
        return ResponseEntity.badRequest().body(
                ResDTO.builder()
                        .code(ResCode.BAD_REQUEST_EXCEPTION)
                        .message(e.getMessage())
                        .build()
        );
    }

    // ===============================================================
    // ✅ 2. 인증/인가 관련 예외
    // ===============================================================

    /**
     * 권한이 없는 경우 (인가 실패)
     */
    @ExceptionHandler(AuthorityException.class)
    public ResponseEntity<ResDTO<Object>> handleAuthorityException(Exception e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ResDTO.builder()
                        .code(ResCode.AUTHORITY_EXCEPTION)
                        .message("권한이 없습니다.")
                        .build()
        );
    }

    // ===============================================================
    // ✅ 3. 비즈니스/도메인 로직 예외
    // ===============================================================

    /**
     * 이미 존재하는 엔티티에 대한 예외 (e.g. 중복된 이메일, 닉네임 등)
     */
    @ExceptionHandler(EntityAlreadyExistException.class)
    public ResponseEntity<ResDTO<Object>> handleEntityAlreadyExistException(Exception e) {
        return ResponseEntity.badRequest().body(
                ResDTO.builder()
                        .code(ResCode.ENTITY_ALREADY_EXIST_EXCEPTION)
                        .message(e.getMessage())
                        .build()
        );
    }

    /**
     * 존재하지 않는 엔티티에 대한 예외 (e.g. 없는 유저, 게시글 등)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResDTO<Object>> handleEntityNotFoundException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResDTO.builder()
                        .code(ResCode.NOT_FOUND_EXCEPTION)
                        .message(e.getMessage())
                        .build()
        );
    }

    // ===============================================================
    // ✅ 4. 기타 예상하지 못한 시스템 예외
    // ===============================================================

    /**
     * 처리되지 않은 모든 예외 (디버깅 용도 포함)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResDTO<Object>> handleException(Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ResDTO.builder()
                        .code(ResCode.EXCEPTION)
                        .message(e.getMessage())
                        .build()
        );
    }
}