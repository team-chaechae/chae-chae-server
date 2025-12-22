package com.project.common.kafka.backpressure;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BackpressureStatus {
    private final int maxPermits;
    private final int availablePermits;
    private final int inFlightCount;
    private final int waitingCount;
    private final int poolSize;
    private final int activeCount;
    private final int queueSize;
}
