package com.project.paymentservice.infrastructure.kafka.backpressure;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/backpressure")
@RequiredArgsConstructor
public class BackpressureController {

    private final BlockingThreadPoolExecutor kafkaBackpressureExecutor;

    @GetMapping("/status")
    public ResponseEntity<BlockingThreadPoolExecutor.BackpressureStatus> getStatus() {
        return ResponseEntity.ok(kafkaBackpressureExecutor.getStatus());
    }

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
