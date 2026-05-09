package com.project.common.dlq.consumer;

import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.domain.DlqRecord;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.common.dlq.domain.DlqStatus;
import com.project.common.dlq.producer.DlqMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DLQ 리플레이 서비스
 *
 * 비즈니스 오류 수동 리플레이 및 정리 작업을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqReplayService {

    private final DlqRecordRepository dlqRecordRepository;
    private final DlqMessageProducer dlqProducer;
    private final DlqProperties properties;

    /**
     * PENDING 레코드 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<DlqRecord> getPendingRecords(Pageable pageable) {
        return dlqRecordRepository.findByStatus(DlqStatus.PENDING, pageable);
    }

    /**
     * 상태별 레코드 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<DlqRecord> getRecordsByStatus(DlqStatus status, Pageable pageable) {
        return dlqRecordRepository.findByStatus(status, pageable);
    }

    /**
     * 서비스 및 상태별 레코드 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<DlqRecord> getRecordsByServiceAndStatus(
            String serviceName, DlqStatus status, Pageable pageable) {
        return dlqRecordRepository.findByStatusAndServiceName(status, serviceName, pageable);
    }

    /**
     * 단건 리플레이
     */
    @Transactional
    public void replayRecord(Long id) {
        DlqRecord record = dlqRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ 레코드를 찾을 수 없습니다: " + id));

        if (record.getStatus() != DlqStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태의 레코드만 리플레이할 수 있습니다. 현재 상태: " + record.getStatus());
        }

        record.startRetry();

        try {
            DlqMessage message = DlqMessage.builder()
                    .originalTopic(record.getOriginalTopic())
                    .originalKey(record.getOriginalKey())
                    .originalPayload(record.getOriginalPayload())
                    .build();

            boolean replayed = dlqProducer.replayToOriginalSync(message);
            if (!replayed) {
                throw new IllegalStateException("Kafka 재발행 실패");
            }

            record.markAsResolved();
            log.info("[DLQ Replay] 수동 리플레이 성공 - id: {}, topic: {}",
                    id, record.getOriginalTopic());

        } catch (Exception e) {
            record.markAsPending();
            log.error("[DLQ Replay] 수동 리플레이 실패 - id: {}, error: {}",
                    id, e.getMessage());
            throw new RuntimeException("리플레이 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 배치 리플레이
     */
    @Transactional
    public ReplayResult replayBatch(List<Long> ids) {
        int success = 0;
        int failed = 0;

        for (Long id : ids) {
            try {
                replayRecord(id);
                success++;
            } catch (Exception e) {
                log.warn("[DLQ Replay] 배치 리플레이 개별 실패 - id: {}, error: {}",
                        id, e.getMessage());
                failed++;
            }
        }

        return new ReplayResult(success, failed);
    }

    /**
     * 레코드 폐기
     */
    @Transactional
    public void discardRecord(Long id, String reason) {
        DlqRecord record = dlqRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ 레코드를 찾을 수 없습니다: " + id));

        record.discard(reason);
        log.info("[DLQ Replay] 레코드 폐기 - id: {}, reason: {}", id, reason);
    }

    /**
     * 오래된 레코드 정리 (스케줄러)
     */
    @Scheduled(cron = "${dlq.cleanup.schedule-cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupOldRecords() {
        if (!properties.getCleanup().isEnabled()) {
            return;
        }

        int retentionDays = properties.getCleanup().getRetentionDays();
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);

        int deleted = dlqRecordRepository.deleteOldRecords(
                List.of(DlqStatus.RESOLVED, DlqStatus.DISCARDED),
                before
        );

        log.info("[DLQ Cleanup] {}일 이전 완료/폐기 레코드 {}건 삭제",
                retentionDays, deleted);
    }

    /**
     * 리플레이 결과
     */
    public record ReplayResult(int success, int failed) {
        public int total() {
            return success + failed;
        }
    }
}
