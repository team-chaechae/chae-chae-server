package com.project.orderservice.infrastructure.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 관리 레지스트리
 *
 * orderId별로 SSE 연결을 관리하여 실시간 알림 전송
 * - 클라이언트가 결제 요청 후 SSE 구독
 * - 재고 차감 실패 시 해당 orderId로 알림 전송
 */
@Slf4j
@Component
public class SseEmitterRegistry {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5분

    // orderId → SseEmitter 매핑
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결 등록
     */
    public SseEmitter register(String orderId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("[SSE] 연결 완료 - orderId: {}", orderId);
            emitters.remove(orderId);
        });

        emitter.onTimeout(() -> {
            log.debug("[SSE] 타임아웃 - orderId: {}", orderId);
            emitters.remove(orderId);
        });

        emitter.onError(e -> {
            log.warn("[SSE] 에러 발생 - orderId: {}, error: {}", orderId, e.getMessage());
            emitters.remove(orderId);
        });

        emitters.put(orderId, emitter);
        log.info("[SSE] 연결 등록 - orderId: {}, 현재 연결 수: {}", orderId, emitters.size());

        return emitter;
    }

    /**
     * 특정 orderId로 이벤트 전송
     */
    public void sendEvent(String orderId, NotificationEvent event) {
        SseEmitter emitter = emitters.get(orderId);

        if (emitter == null) {
            log.debug("[SSE] 연결 없음 - orderId: {}", orderId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(event.getEventType())
                    .data(event));
            log.info("[SSE] 이벤트 전송 완료 - orderId: {}, eventType: {}", orderId, event.getEventType());
        } catch (IOException e) {
            log.warn("[SSE] 전송 실패 - orderId: {}, error: {}", orderId, e.getMessage());
            emitters.remove(orderId);
        }
    }

    /**
     * 연결 해제
     */
    public void remove(String orderId) {
        SseEmitter emitter = emitters.remove(orderId);
        if (emitter != null) {
            emitter.complete();
            log.debug("[SSE] 연결 해제 - orderId: {}", orderId);
        }
    }

    /**
     * 현재 연결 수 조회 (모니터링용)
     */
    public int getConnectionCount() {
        return emitters.size();
    }
}
