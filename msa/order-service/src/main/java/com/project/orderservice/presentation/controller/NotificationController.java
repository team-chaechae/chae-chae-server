package com.project.orderservice.presentation.controller;

import com.project.orderservice.infrastructure.sse.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 알림 구독 컨트롤러
 *
 * 클라이언트가 결제 요청 후 SSE 연결을 통해 실시간 알림 수신
 * - 재고 차감 실패 시 INVENTORY_FAILED 이벤트 전송
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "실시간 알림 API")
public class NotificationController {

    private final SseEmitterRegistry sseEmitterRegistry;

    /**
     * SSE 구독 - 주문 상태 알림
     *
     * 클라이언트는 결제 요청 후 이 엔드포인트로 SSE 연결
     * "결제중입니다..." 메시지를 표시하며 대기
     * 재고 차감 완료 또는 실패 시 이벤트 수신
     */
    @GetMapping(value = "/subscribe/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "주문 상태 SSE 구독", description = "주문에 대한 실시간 상태 알림을 SSE로 수신")
    public SseEmitter subscribe(@PathVariable String orderId) {
        log.info("[SSE 구독 요청] orderId: {}", orderId);
        return sseEmitterRegistry.register(orderId);
    }

    /**
     * 현재 SSE 연결 수 조회 (모니터링용)
     */
    @GetMapping("/connections")
    @Operation(summary = "SSE 연결 수 조회", description = "현재 활성화된 SSE 연결 수 조회")
    public int getConnectionCount() {
        return sseEmitterRegistry.getConnectionCount();
    }
}
