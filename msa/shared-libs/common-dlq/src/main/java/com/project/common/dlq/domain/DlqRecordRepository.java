package com.project.common.dlq.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DLQ 레코드 Repository
 */
public interface DlqRecordRepository extends JpaRepository<DlqRecord, Long> {

    /**
     * 상태별 조회 (페이징)
     */
    Page<DlqRecord> findByStatus(DlqStatus status, Pageable pageable);

    /**
     * 상태 및 서비스명별 조회 (페이징)
     */
    Page<DlqRecord> findByStatusAndServiceName(DlqStatus status, String serviceName, Pageable pageable);

    /**
     * 원본 토픽별 조회 (페이징)
     */
    Page<DlqRecord> findByOriginalTopic(String originalTopic, Pageable pageable);

    /**
     * PENDING 상태 레코드 목록 조회
     */
    List<DlqRecord> findByStatusOrderByCreatedAtAsc(DlqStatus status);

    /**
     * 기간 내 레코드 수 조회
     */
    @Query("SELECT COUNT(d) FROM DlqRecord d WHERE d.status = :status AND d.createdAt >= :since")
    long countByStatusSince(@Param("status") DlqStatus status, @Param("since") LocalDateTime since);

    /**
     * 오래된 RESOLVED/DISCARDED 레코드 삭제
     */
    @Modifying
    @Query("DELETE FROM DlqRecord d WHERE d.status IN (:statuses) AND d.resolvedAt < :before")
    int deleteOldRecords(@Param("statuses") List<DlqStatus> statuses, @Param("before") LocalDateTime before);

    /**
     * 서비스별 PENDING 레코드 수 조회
     */
    @Query("SELECT d.serviceName, COUNT(d) FROM DlqRecord d WHERE d.status = 'PENDING' GROUP BY d.serviceName")
    List<Object[]> countPendingByService();

    /**
     * 토픽별 PENDING 레코드 수 조회
     */
    @Query("SELECT d.originalTopic, COUNT(d) FROM DlqRecord d WHERE d.status = 'PENDING' GROUP BY d.originalTopic")
    List<Object[]> countPendingByTopic();
}
