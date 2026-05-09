package com.project.paymentservice.infrastructure.kafka.backpressure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Semaphore 기반 백프레셔가 내장된 ThreadPoolExecutor
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
        blockUntilAcquireSemaphore();

        try {
            super.execute(() -> {
                try {
                    command.run();
                } finally {
                    releaseSemaphore();
                }
            });
        } catch (RejectedExecutionException ex) {
            log.error("[BlockingThreadPool] 작업 거부", ex);
            releaseSemaphore();
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
            log.warn("[BlockingThreadPool] Semaphore acquire 타임아웃 ({}ms)", acquireTimeoutMs);
            throw new RejectedExecutionException("Semaphore acquire timeout");
        }

        inFlightCount.incrementAndGet();
    }

    private void releaseSemaphore() {
        semaphore.release();
        inFlightCount.decrementAndGet();
    }

    public synchronized void adjustMaxPermits(int newMaxPermits) {
        int oldMaxPermits = currentMaxPermits.get();
        int diff = newMaxPermits - oldMaxPermits;

        if (diff > 0) {
            semaphore.release(diff);
        } else if (diff < 0) {
            int currentAvailable = semaphore.availablePermits();
            int newAvailable = Math.max(0, currentAvailable + diff);
            this.semaphore = new Semaphore(newAvailable);
        }

        currentMaxPermits.set(newMaxPermits);
        log.info("[BlockingThreadPool] maxPermits 변경: {} -> {}", oldMaxPermits, newMaxPermits);
    }

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
            shutdownNow();
        }
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
