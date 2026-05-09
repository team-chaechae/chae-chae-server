package com.project.common.dlq.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DLQ 레코드 엔티티 (비즈니스 오류 DB 저장용)
 */
@Entity
@Table(name = "dlq_records", indexes = {
        @Index(name = "idx_dlq_status", columnList = "status"),
        @Index(name = "idx_dlq_created_at", columnList = "createdAt"),
        @Index(name = "idx_dlq_original_topic", columnList = "originalTopic")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DlqRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 토픽
     */
    @Column(nullable = false, length = 255)
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
    @Column(length = 500)
    private String originalKey;

    /**
     * 원본 페이로드
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String originalPayload;

    /**
     * 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DlqStatus status;

    /**
     * 예외 타입
     */
    @Column(length = 255)
    private String exceptionType;

    /**
     * 예외 메시지
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String exceptionMessage;

    /**
     * 스택 트레이스
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    /**
     * 서비스명
     */
    @Column(nullable = false, length = 100)
    private String serviceName;

    /**
     * Consumer Group ID
     */
    @Column(length = 255)
    private String consumerGroup;

    /**
     * 재시도 횟수
     */
    @Column(nullable = false)
    private int retryCount;

    /**
     * 생성 시간
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 해결 시간
     */
    private LocalDateTime resolvedAt;

    /**
     * 메모 (관리자 코멘트)
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String memo;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DlqStatus.PENDING;
        }
        if (this.retryCount == 0) {
            this.retryCount = 0;
        }
    }

    /**
     * 재시도 시작
     */
    public void startRetry() {
        this.status = DlqStatus.RETRYING;
        this.retryCount++;
    }

    /**
     * 재처리 완료
     */
    public void markAsResolved() {
        this.status = DlqStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * 재시도 실패 (다시 PENDING으로)
     */
    public void markAsPending() {
        this.status = DlqStatus.PENDING;
    }

    /**
     * 폐기
     */
    public void discard(String reason) {
        this.status = DlqStatus.DISCARDED;
        this.resolvedAt = LocalDateTime.now();
        this.memo = reason;
    }

    /**
     * DlqMessage에서 DlqRecord 생성
     */
    public static DlqRecord from(DlqMessage message) {
        return DlqRecord.builder()
                .originalTopic(message.getOriginalTopic())
                .originalPartition(message.getOriginalPartition())
                .originalOffset(message.getOriginalOffset())
                .originalKey(message.getOriginalKey())
                .originalPayload(message.getOriginalPayload())
                .status(DlqStatus.PENDING)
                .exceptionType(message.getExceptionType())
                .exceptionMessage(message.getExceptionMessage())
                .stackTrace(message.getStackTrace())
                .serviceName(message.getServiceName())
                .consumerGroup(message.getConsumerGroup())
                .retryCount(message.getRetryCount())
                .build();
    }
}
