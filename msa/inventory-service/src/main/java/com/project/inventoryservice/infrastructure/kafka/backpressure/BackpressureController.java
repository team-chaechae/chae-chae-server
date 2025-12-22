package com.project.inventoryservice.infrastructure.kafka.backpressure;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 백프레셔 Runtime 조절 API
 */
@RestController
@RequestMapping("/api/internal/backpressure")
@RequiredArgsConstructor
public class BackpressureController {

    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;

    /**
     * 현재 백프레셔 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<BlockingThreadPoolExecutor.BackpressureStatus> getStatus() {
        return ResponseEntity.ok(kafkaBackpressureExecutor.getStatus());
    }

    /**
     * maxPermits 조절 (Runtime)
     */
    @PostMapping("/max-permits")
    public ResponseEntity<BlockingThreadPoolExecutor.BackpressureStatus> adjustMaxPermits(
            @RequestParam int value) {
        if (value < 1 || value > 1000) {
            return ResponseEntity.badRequest().build();
        }
        kafkaBackpressureExecutor.adjustMaxPermits(value);
        return ResponseEntity.ok(kafkaBackpressureExecutor.getStatus());
    }
}
