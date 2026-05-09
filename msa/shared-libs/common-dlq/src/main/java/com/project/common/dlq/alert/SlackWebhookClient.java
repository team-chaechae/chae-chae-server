package com.project.common.dlq.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Slack 웹훅 클라이언트 (DLQ 전용)
 */
@Slf4j
@Component
public class SlackWebhookClient {

    private final RestClient restClient;
    private final String webhookUrl;
    private final String serviceName;
    private final boolean enabled;

    public SlackWebhookClient(
            @Value("${slack.webhook.url:}") String webhookUrl,
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${slack.alert.enabled:false}") boolean enabled) {
        this.restClient = RestClient.create();
        this.webhookUrl = webhookUrl;
        this.serviceName = serviceName;
        this.enabled = enabled;
    }

    /**
     * DLQ 알림 전송
     */
    @Async
    public void sendDlqAlert(String title, String description) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[DLQ Slack] 비활성화 상태");
            return;
        }

        String message = String.format("""
                :rotating_light: *%s*
                *Service:* `%s`
                *Time:* %s
                %s
                """,
                title,
                serviceName,
                getCurrentTime(),
                description
        );

        try {
            Map<String, String> payload = Map.of("text", message);

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[DLQ Slack] 알림 전송 성공 - {}", title);
        } catch (Exception e) {
            log.error("[DLQ Slack] 알림 전송 실패: {}", e.getMessage());
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
