package com.project.common.dlq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DLQ 설정 속성
 */
@Data
@ConfigurationProperties(prefix = "dlq")
public class DlqProperties {

    /**
     * DLQ 기능 활성화 여부
     */
    private boolean enabled = true;

    /**
     * DLQ 토픽 접미사
     */
    private String topicSuffix = ".dlq";

    /**
     * 최대 재시도 횟수
     */
    private int maxRetries = 3;

    /**
     * 재시도 간격 (밀리초)
     */
    private long retryIntervalMs = 1000;

    /**
     * 재시도 간격 배수 (Exponential Backoff)
     */
    private double retryMultiplier = 2.0;

    /**
     * Slack 알림 활성화 여부
     */
    private boolean alertEnabled = true;

    /**
     * DB 저장 활성화 여부 (비즈니스 오류)
     */
    private boolean dbEnabled = true;

    /**
     * 기술적 오류 자동 리플레이 설정
     */
    private TechnicalReplay technicalReplay = new TechnicalReplay();

    /**
     * 정리 작업 설정
     */
    private Cleanup cleanup = new Cleanup();

    /**
     * 알림 임계치 설정
     */
    private AlertThreshold alertThreshold = new AlertThreshold();

    /**
     * 기술적 오류 자동 리플레이 설정
     */
    @Data
    public static class TechnicalReplay {
        /**
         * 자동 리플레이 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 자동 리플레이 스케줄 (Cron 표현식)
         */
        private String scheduleCron = "0 */5 * * * ?";  // 5분마다

        /**
         * 한 번에 리플레이할 메시지 수
         */
        private int batchSize = 100;

        /**
         * 최대 리플레이 재시도 횟수
         */
        private int maxReplayRetries = 3;
    }

    /**
     * 정리 작업 설정
     */
    @Data
    public static class Cleanup {
        /**
         * 정리 작업 활성화 여부
         */
        private boolean enabled = true;

        /**
         * 정리 작업 스케줄 (Cron 표현식)
         */
        private String scheduleCron = "0 0 3 * * ?";  // 매일 새벽 3시

        /**
         * RESOLVED/DISCARDED 레코드 보관 기간 (일)
         */
        private int retentionDays = 30;
    }

    /**
     * 알림 임계치 설정
     */
    @Data
    public static class AlertThreshold {
        /**
         * PENDING 레코드 수 임계치 (이 수를 초과하면 알림)
         */
        private int pendingCount = 100;

        /**
         * 알림 체크 스케줄 (Cron 표현식)
         */
        private String checkCron = "0 0 * * * ?";  // 매시 정각
    }
}
