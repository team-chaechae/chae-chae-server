package com.project.chaechaeserver.application.global.constants;

import lombok.Getter;

import java.util.Map;

@Getter
public class ResCode {

    public static final int NO_CONTENT = 3; // NOTE : 204 응답 바디 없음
    public static final int ACCEPTED = 2; // NOTE 202 요청 수락, 비동기 처리 중
    public static final int CREATED = 1; // NOTE : 201 리소스 생성 성공
    public static final int OK = 0; // NOTE : 200 일반 성공
    public static final int BAD_REQUEST_EXCEPTION = -1; // NOTE : 잘못된 요청
    public static final int ENTITY_ALREADY_EXIST_EXCEPTION = -2;// NOTE : 중복 객체
    public static final int NOT_FOUND_EXCEPTION = -3; // NOTE : 존재하지 않는 객체
    public static final int HTTP_MESSAGE_NOT_READABLE_EXCEPTION = -4; // NOTE : JSON 파싱 불가
    public static final int METHOD_ARGUMENT_TYPE_MISMATCH_EXCEPTION = -5; // NOTE : 파라미터 타입 불일치
    public static final int AUTHORITY_EXCEPTION = -6; // NOTE : 권한 검증
    public static final int EXCEPTION = -99; // NOTE : 기타 에러

    private static final Map<String, Integer> CODE_MAP = Map.of(
            "200", OK,
            "201", CREATED,
            "202", ACCEPTED,
            "204", NO_CONTENT
    );

    public static int from(String httpStatusCode) {
        return CODE_MAP.getOrDefault(httpStatusCode, OK);
    }
}