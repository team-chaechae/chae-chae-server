package com.project.inventoryservice.infrastructure.kafka.backpressure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Semaphore 기반 백프레셔가 내장된 ThreadPoolExecutor
 *
 * - execute() 호출 시 Semaphore acquire (permit 없으면 Block)
 * - 작업 완료 시 자동으로 release
 * - Runtime에서 maxPermit 조절 가능
 */
@Slf4j
public class BlockingThreadPoolExecutor extends ThreadPoolExecutor implements DisposableBean {

    private volatile Semaphore semaphore;
    private final AtomicInteger currentMaxPermits = new AtomicInteger();
    private final AtomicInteger inFlightCount = new AtomicInteger(0);
    private final long acquireTimeoutMs;

    public BlockingThreadPoolExecutor(
            int corePoolSize,
            int maxPoolSize,
            int maxPermits,
            long acquireTimeoutMs,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory
    ) {
        super(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, workQueue, threadFactory);
        this.semaphore = new Semaphore(maxPermits);
        this.currentMaxPermits.set(maxPermits);
        this.acquireTimeoutMs = acquireTimeoutMs;
        log.info("[BlockingThreadPool] 초기화 - corePoolSize: {}, maxPoolSize: {}, maxPermits: {}",
                corePoolSize, maxPoolSize, maxPermits);
    }

    @Override
    public void execute(Runnable command) {
        // 1. Semaphore acquire (Block if no permits)
        blockUntilAcquireSemaphore();

        try {
            // 2. ThreadPool에 작업 제출
            super.execute(() -> {
                try {
                    command.run();
                } finally {
                    // 3. 작업 완료 시 release
                    releaseSemaphore();
                }
            });
        } catch (RejectedExecutionException ex) {
            log.error("[BlockingThreadPool] 작업 거부 - 이 에러는 발생하면 안됨", ex);
            releaseSemaphore();  // acquire 했으니 release
            throw ex;
        }
    }

    private void blockUntilAcquireSemaphore() {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Semaphore acquire interrupted", e);
        }

        if (!acquired) {
            log.warn("[BlockingThreadPool] Semaphore acquire 타임아웃 ({}ms) - inFlight: {}, waiting: {}",
                    acquireTimeoutMs, inFlightCount.get(), semaphore.getQueueLength());
            throw new RejectedExecutionException("Semaphore acquire timeout after " + acquireTimeoutMs + "ms");
        }

        int current = inFlightCount.incrementAndGet();
        if (current % 10 == 0) {
            log.debug("[BlockingThreadPool] acquire - inFlight: {}, available: {}",
                    current, semaphore.availablePermits());
        }
    }

    private void releaseSemaphore() {
        semaphore.release();
        int current = inFlightCount.decrementAndGet();
        if (current % 10 == 0) {
            log.debug("[BlockingThreadPool] release - inFlight: {}, available: {}",
                    current, semaphore.availablePermits());
        }
    }

    /**
     * Runtime에서 maxPermits 조절
     */
    public synchronized void adjustMaxPermits(int newMaxPermits) {
        int oldMaxPermits = currentMaxPermits.get();
        int diff = newMaxPermits - oldMaxPermits;

        if (diff > 0) {
            semaphore.release(diff);
        } else if (diff < 0) {
            // 새 Semaphore로 교체 (기존 작업 완료 후 반영)
            int currentAvailable = semaphore.availablePermits();
            int newAvailable = Math.max(0, currentAvailable + diff);
            this.semaphore = new Semaphore(newAvailable);
        }

        currentMaxPermits.set(newMaxPermits);
        log.info("[BlockingThreadPool] maxPermits 변경: {} -> {}", oldMaxPermits, newMaxPermits);
    }

    /**
     * 상태 조회
     */
    public BackpressureStatus getStatus() {
        return BackpressureStatus.builder()
                .maxPermits(currentMaxPermits.get())
                .availablePermits(semaphore.availablePermits())
                .inFlightCount(inFlightCount.get())
                .waitingCount(semaphore.getQueueLength())
                .poolSize(getPoolSize())
                .activeCount(getActiveCount())
                .queueSize(getQueue().size())
                .build();
    }

    @Override
    public void destroy() throws Exception {
        log.info("[BlockingThreadPool] 종료 시작");
        shutdown();
        if (!awaitTermination(30, TimeUnit.SECONDS)) {
            log.warn("[BlockingThreadPool] 강제 종료");
            shutdownNow();
        }
        log.info("[BlockingThreadPool] 종료 완료");
    }

    @lombok.Builder
    @lombok.Getter
    public static class BackpressureStatus {
        private final int maxPermits;
        private final int availablePermits;
        private final int inFlightCount;
        private final int waitingCount;
        private final int poolSize;
        private final int activeCount;
        private final int queueSize;
    }
}
