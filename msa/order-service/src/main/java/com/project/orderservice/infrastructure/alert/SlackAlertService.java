package com.project.orderservice.infrastructure.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class SlackAlertService {

    private final RestClient restClient;
    private final String webhookUrl;
    private final String serviceName;
    private final boolean enabled;

    public SlackAlertService(
            @Value("${slack.webhook.url:}") String webhookUrl,
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${slack.alert.enabled:false}") boolean enabled) {
        this.restClient = RestClient.create();
        this.webhookUrl = webhookUrl;
        this.serviceName = serviceName;
        this.enabled = enabled;
    }

    @Async
    public void sendKafkaErrorAlert(String topic, String errorMessage, Exception e) {
        String message = buildKafkaErrorMessage(topic, errorMessage, e);
        sendAlert(message);
    }

    @Async
    public void sendApiErrorAlert(String endpoint, String errorMessage, Exception e) {
        String message = buildApiErrorMessage(endpoint, errorMessage, e);
        sendAlert(message);
    }

    @Async
    public void sendCustomAlert(String title, String description) {
        String message = buildCustomMessage(title, description);
        sendAlert(message);
    }

    private void sendAlert(String message) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[Slack Alert] 비활성화 상태 - message: {}", message);
            return;
        }

        try {
            Map<String, String> payload = Map.of("text", message);

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[Slack Alert] 전송 성공");
        } catch (Exception e) {
            log.error("[Slack Alert] 전송 실패: {}", e.getMessage());
        }
    }

    private String buildKafkaErrorMessage(String topic, String errorMessage, Exception e) {
        return String.format("""
                :rotating_light: *Kafka Consumer Error*
                *Service:* `%s`
                *Topic:* `%s`
                *Time:* %s
                *Error:* %s
                *Exception:* `%s`
                """,
                serviceName,
                topic,
                getCurrentTime(),
                errorMessage,
                e != null ? e.getClass().getSimpleName() + ": " + e.getMessage() : "N/A"
        );
    }

    private String buildApiErrorMessage(String endpoint, String errorMessage, Exception e) {
        return String.format("""
                :warning: *API Error (5xx)*
                *Service:* `%s`
                *Endpoint:* `%s`
                *Time:* %s
                *Error:* %s
                *Exception:* `%s`
                """,
                serviceName,
                endpoint,
                getCurrentTime(),
                errorMessage,
                e != null ? e.getClass().getSimpleName() + ": " + e.getMessage() : "N/A"
        );
    }

    private String buildCustomMessage(String title, String description) {
        return String.format("""
                :bell: *%s*
                *Service:* `%s`
                *Time:* %s
                *Details:* %s
                """,
                title,
                serviceName,
                getCurrentTime(),
                description
        );
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
