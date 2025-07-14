package com.project.chaechaeserver.application.global.excepion;

import lombok.Getter;

@Getter
public class ErrorCode {

    public static final int BAD_REQUEST_EXCEPTION = -1; // NOTE : 잘못된 요청
    public static final int ENTITY_ALREADY_EXIST_EXCEPTION =  -2;// NOTE : 중복 객체
    public static final int NOT_FOUND_EXCEPTION = -3; // NOTE : 존재하지 않는 객체
    public static final int HTTP_MESSAGE_NOT_READABLE_EXCEPTION = -4; // NOTE : JSON 파싱 불가
    public static final int METHOD_ARGUMENT_TYPE_MISMATCH_EXCEPTION = -5; // NOTE : 파라미터 타입 불일치
    public static final int AUTHORITY_EXCEPTION = -6; // NOTE : 권한 검증
    public static final int EXCEPTION = -99; // NOTE : 기타 에러

}