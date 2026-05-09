package com.project.common.dlq.consumer;

import com.project.common.dlq.domain.DlqRecord;
import com.project.common.dlq.domain.DlqStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DLQ 리플레이 REST API Controller
 *
 * 비즈니스 오류 수동 확인 및 리플레이를 위한 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dlq", name = "db-enabled", havingValue = "true", matchIfMissing = true)
public class DlqReplayController {

    private final DlqReplayService replayService;

    /**
     * PENDING 레코드 목록 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<DlqRecordDTO>> getPendingRecords(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<DlqRecord> records = replayService.getPendingRecords(pageable);
        return ResponseEntity.ok(records.map(DlqRecordDTO::from));
    }

    /**
     * 상태별 레코드 목록 조회
     */
    @GetMapping
    public ResponseEntity<Page<DlqRecordDTO>> getRecords(
            @RequestParam(required = false, defaultValue = "PENDING") DlqStatus status,
            @RequestParam(required = false) String serviceName,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<DlqRecord> records;
        if (serviceName != null && !serviceName.isBlank()) {
            records = replayService.getRecordsByServiceAndStatus(serviceName, status, pageable);
        } else {
            records = replayService.getRecordsByStatus(status, pageable);
        }
        return ResponseEntity.ok(records.map(DlqRecordDTO::from));
    }

    /**
     * 단건 리플레이
     */
    @PostMapping("/{id}/replay")
    public ResponseEntity<Void> replayRecord(@PathVariable Long id) {
        log.info("[DLQ API] 단건 리플레이 요청 - id: {}", id);
        replayService.replayRecord(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 배치 리플레이
     */
    @PostMapping("/replay-batch")
    public ResponseEntity<ReplayResultDTO> replayBatch(@RequestBody ReplayBatchRequest request) {
        log.info("[DLQ API] 배치 리플레이 요청 - ids: {}", request.ids());
        DlqReplayService.ReplayResult result = replayService.replayBatch(request.ids());
        return ResponseEntity.ok(new ReplayResultDTO(result.success(), result.failed(), result.total()));
    }

    /**
     * 레코드 폐기
     */
    @PostMapping("/{id}/discard")
    public ResponseEntity<Void> discardRecord(
            @PathVariable Long id,
            @RequestBody(required = false) DiscardRequest request) {

        String reason = request != null ? request.reason() : "관리자에 의해 폐기됨";
        log.info("[DLQ API] 레코드 폐기 요청 - id: {}, reason: {}", id, reason);
        replayService.discardRecord(id, reason);
        return ResponseEntity.ok().build();
    }

    // ===== DTO =====

    public record DlqRecordDTO(
            Long id,
            String originalTopic,
            Integer originalPartition,
            Long originalOffset,
            String originalKey,
            String originalPayload,
            DlqStatus status,
            String exceptionType,
            String exceptionMessage,
            String serviceName,
            int retryCount,
            String createdAt,
            String resolvedAt,
            String memo
    ) {
        public static DlqRecordDTO from(DlqRecord record) {
            return new DlqRecordDTO(
                    record.getId(),
                    record.getOriginalTopic(),
                    record.getOriginalPartition(),
                    record.getOriginalOffset(),
                    record.getOriginalKey(),
                    truncate(record.getOriginalPayload(), 500),
                    record.getStatus(),
                    record.getExceptionType(),
                    truncate(record.getExceptionMessage(), 300),
                    record.getServiceName(),
                    record.getRetryCount(),
                    record.getCreatedAt() != null ? record.getCreatedAt().toString() : null,
                    record.getResolvedAt() != null ? record.getResolvedAt().toString() : null,
                    record.getMemo()
            );
        }

        private static String truncate(String str, int maxLength) {
            if (str == null) return null;
            if (str.length() <= maxLength) return str;
            return str.substring(0, maxLength) + "...";
        }
    }

    public record ReplayBatchRequest(List<Long> ids) {}

    public record DiscardRequest(String reason) {}

    public record ReplayResultDTO(int success, int failed, int total) {}
}
