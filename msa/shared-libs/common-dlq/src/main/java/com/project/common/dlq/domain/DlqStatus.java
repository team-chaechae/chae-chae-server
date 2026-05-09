package com.project.common.dlq.domain;

/**
 * DLQ 레코드 상태
 */
public enum DlqStatus {
    /**
     * 대기 중 (수동 확인 필요)
     */
    PENDING,

    /**
     * 재처리 시도 중
     */
    RETRYING,

    /**
     * 재처리 완료
     */
    RESOLVED,

    /**
     * 폐기됨 (수동 폐기)
     */
    DISCARDED
}
