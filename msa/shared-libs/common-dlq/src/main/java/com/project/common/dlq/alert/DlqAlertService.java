package com.project.common.dlq.alert;

import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.common.dlq.domain.DlqStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DLQ 알림 서비스
 *
 * 비즈니스 오류 발생 시 즉시 알림 및 임계치 초과 시 알림을 전송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DlqAlertService {

    private final SlackWebhookClient slackWebhookClient;
    private final DlqRecordRepository dlqRecordRepository;
    private final DlqProperties properties;

    /**
     * DLQ 메시지 발생 알림 (비즈니스 오류)
     */
    public void sendDlqAlert(DlqMessage message) {
        if (!properties.isAlertEnabled()) {
            return;
        }

        // 비즈니스 오류만 즉시 알림
        if (message.getExceptionCategory() == DlqMessage.ExceptionCategory.BUSINESS) {
            String title = "DLQ 비즈니스 오류 발생";
            String description = buildAlertDescription(message);
            slackWebhookClient.sendDlqAlert(title, description);
        }
    }

    /**
     * 기술적 오류 알림 (자동 리플레이 실패 시)
     */
    public void sendTechnicalReplayFailedAlert(DlqMessage message, int retryCount) {
        if (!properties.isAlertEnabled()) {
            return;
        }

        String title = "DLQ 기술적 오류 - 자동 리플레이 실패";
        String description = String.format("""
                Topic: `%s`
                Key: `%s`
                예외: `%s`
                메시지: %s
                리플레이 시도: %d회
                """,
                message.getOriginalTopic(),
                message.getOriginalKey(),
                message.getExceptionType(),
                truncate(message.getExceptionMessage(), 200),
                retryCount
        );
        slackWebhookClient.sendDlqAlert(title, description);
    }

    /**
     * PENDING 레코드 임계치 초과 알림 (스케줄러)
     */
    @Scheduled(cron = "${dlq.alert-threshold.check-cron:0 0 * * * ?}")
    public void checkPendingThreshold() {
        if (!properties.isAlertEnabled() || !properties.isDbEnabled()) {
            return;
        }

        long pendingCount = dlqRecordRepository.countByStatusSince(
                DlqStatus.PENDING,
                LocalDateTime.now().minusHours(24)
        );

        int threshold = properties.getAlertThreshold().getPendingCount();

        if (pendingCount > threshold) {
            String title = "DLQ PENDING 레코드 임계치 초과";
            String description = String.format("""
                    PENDING 레코드 수: %d건
                    임계치: %d건
                    확인이 필요합니다.
                    """,
                    pendingCount,
                    threshold
            );
            slackWebhookClient.sendDlqAlert(title, description);

            // 서비스별 현황도 전송
            sendPendingBreakdown();
        }
    }

    /**
     * 서비스별 PENDING 현황 전송
     */
    private void sendPendingBreakdown() {
        List<Object[]> byService = dlqRecordRepository.countPendingByService();
        List<Object[]> byTopic = dlqRecordRepository.countPendingByTopic();

        StringBuilder sb = new StringBuilder();
        sb.append("*서비스별 현황:*\n");
        for (Object[] row : byService) {
            sb.append(String.format("- %s: %d건\n", row[0], row[1]));
        }

        sb.append("\n*토픽별 현황:*\n");
        for (Object[] row : byTopic) {
            sb.append(String.format("- %s: %d건\n", row[0], row[1]));
        }

        slackWebhookClient.sendDlqAlert("DLQ PENDING 상세 현황", sb.toString());
    }

    /**
     * 알림 설명 생성
     */
    private String buildAlertDescription(DlqMessage message) {
        return String.format("""
                Topic: `%s`
                Partition: %d, Offset: %d
                Key: `%s`
                예외 유형: `%s`
                예외 메시지: %s
                서비스: `%s`
                """,
                message.getOriginalTopic(),
                message.getOriginalPartition(),
                message.getOriginalOffset(),
                message.getOriginalKey(),
                message.getExceptionType(),
                truncate(message.getExceptionMessage(), 200),
                message.getServiceName()
        );
    }

    /**
     * 문자열 자르기
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "N/A";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
