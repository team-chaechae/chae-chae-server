package com.project.common.dlq.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DLQ 메시지 DTO (Kafka 토픽 발행용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessage {

    /**
     * 원본 토픽
     */
    private String originalTopic;

    /**
     * 원본 파티션
     */
    private Integer originalPartition;

    /**
     * 원본 오프셋
     */
    private Long originalOffset;

    /**
     * 원본 키
     */
    private String originalKey;

    /**
     * 원본 페이로드 (JSON 문자열)
     */
    private String originalPayload;

    /**
     * 예외 유형 (기술적/비즈니스)
     */
    private ExceptionCategory exceptionCategory;

    /**
     * 예외 클래스명
     */
    private String exceptionType;

    /**
     * 예외 메시지
     */
    private String exceptionMessage;

    /**
     * 스택 트레이스
     */
    private String stackTrace;

    /**
     * 발생 시간
     */
    private LocalDateTime timestamp;

    /**
     * 재시도 횟수
     */
    private int retryCount;

    /**
     * 서비스명
     */
    private String serviceName;

    /**
     * Consumer Group ID
     */
    private String consumerGroup;

    /**
     * 추가 헤더 정보
     */
    private Map<String, String> headers;

    /**
     * 예외 카테고리
     */
    public enum ExceptionCategory {
        /**
         * 기술적 오류 (자동 리플레이 대상)
         */
        TECHNICAL,

        /**
         * 비즈니스 오류 (수동 확인 필요)
         */
        BUSINESS
    }
}
