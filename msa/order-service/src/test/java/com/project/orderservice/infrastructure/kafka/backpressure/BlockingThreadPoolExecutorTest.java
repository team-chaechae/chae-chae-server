package com.project.orderservice.infrastructure.kafka.backpressure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BlockingThreadPoolExecutor 백프레셔 테스트")
class BlockingThreadPoolExecutorTest {

    private BlockingThreadPoolExecutor executor;

    @BeforeEach
    void setUp() {
        // 테스트용 설정: maxPermits=3, timeout=1000ms
        executor = new BlockingThreadPoolExecutor(
                2,      // corePoolSize
                4,      // maxPoolSize
                3,      // maxPermits (동시 처리 최대 3개)
                1000,   // acquireTimeoutMs (1초)
                new LinkedBlockingQueue<>(100),
                Executors.defaultThreadFactory()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.destroy();
    }

    @Test
    @DisplayName("동시 처리 개수가 maxPermits를 초과하지 않는다")
    void execute_RespectsMaxPermits() throws InterruptedException {
        // given
        int maxPermits = 3;
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        CountDownLatch allDone = new CountDownLatch(5);

        // when - 5개 작업 실행 (maxPermits=3이므로 동시에 최대 3개만)
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    executor.execute(() -> {
                        int current = currentConcurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));

                        try {
                            Thread.sleep(50); // 짧은 작업 시뮬레이션
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        currentConcurrent.decrementAndGet();
                    });
                } catch (RejectedExecutionException e) {
                    // 타임아웃으로 거부될 수 있음
                } finally {
                    allDone.countDown();
                }
            }).start();
        }

        // 모든 작업 완료 대기
        allDone.await(10, TimeUnit.SECONDS);
        Thread.sleep(200); // 작업 완료 대기

        // then - 동시 실행 개수가 maxPermits를 초과하지 않아야 함
        assertThat(maxConcurrent.get()).isLessThanOrEqualTo(maxPermits);
    }

    @Test
    @DisplayName("Semaphore가 가득 차면 타임아웃으로 RejectedExecutionException 발생")
    void execute_ThrowsRejectedExecutionException_WhenSemaphoreFull() throws Exception {
        // given - 작은 타임아웃으로 새 executor 생성
        executor.destroy();
        executor = new BlockingThreadPoolExecutor(
                2, 4, 2, 100,  // maxPermits=2, timeout=100ms
                new LinkedBlockingQueue<>(100),
                Executors.defaultThreadFactory()
        );

        CountDownLatch blockLatch = new CountDownLatch(1);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        // Semaphore 가득 채우기 (2개)
        for (int i = 0; i < 2; i++) {
            executor.execute(() -> {
                try {
                    blockLatch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(50); // 태스크들이 시작될 시간

        // when - 3번째 작업은 타임아웃되어야 함
        assertThatThrownBy(() -> executor.execute(() -> {}))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("timeout");

        // cleanup
        blockLatch.countDown();
    }

    @Test
    @DisplayName("Semaphore 대기 시간이 Kafka listener 호출 스레드를 acquireTimeoutMs만큼 블로킹한다")
    void execute_BlocksKafkaListenerThreadUntilAcquireTimeout_WhenSemaphoreFull() throws Exception {
        // given - Kafka listener 스레드가 execute()를 호출하는 상황을 재현한다.
        executor.destroy();
        executor = new BlockingThreadPoolExecutor(
                1, 1, 1, 250,
                new LinkedBlockingQueue<>(100),
                Executors.defaultThreadFactory()
        );

        CountDownLatch releaseRunningTask = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                releaseRunningTask.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(50);

        // when
        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> executor.execute(() -> {}))
                .isInstanceOf(RejectedExecutionException.class)
                .hasMessageContaining("timeout");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        // then - 호출 스레드가 timeout까지 반환하지 못한다. 실제 Kafka consumer 스레드라면 poll/heartbeat도 지연된다.
        assertThat(elapsedMs).isGreaterThanOrEqualTo(220);

        // cleanup
        releaseRunningTask.countDown();
    }

    @Test
    @DisplayName("작업 완료 후 Semaphore가 해제되어 새 작업 수락")
    void execute_ReleasesSemaphore_AfterTaskCompletion() throws InterruptedException {
        // given
        CountDownLatch firstTaskDone = new CountDownLatch(1);
        AtomicInteger completedTasks = new AtomicInteger(0);

        // maxPermits(3) 만큼 작업 실행
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(100);
                    completedTasks.incrementAndGet();
                    firstTaskDone.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 첫 번째 작업이 완료될 때까지 대기
        firstTaskDone.await(5, TimeUnit.SECONDS);

        // when - 새 작업이 수락되어야 함 (RejectedExecutionException 없이)
        CountDownLatch newTaskDone = new CountDownLatch(1);
        executor.execute(newTaskDone::countDown);

        // then
        boolean completed = newTaskDone.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
    }

    @Test
    @DisplayName("getStatus()가 현재 상태를 정확히 반환한다")
    void getStatus_ReturnsCorrectStatus() throws InterruptedException {
        // given
        CountDownLatch blockLatch = new CountDownLatch(1);

        // 2개의 작업 실행
        for (int i = 0; i < 2; i++) {
            executor.execute(() -> {
                try {
                    blockLatch.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(100); // 태스크들이 시작될 시간

        // when
        BlockingThreadPoolExecutor.BackpressureStatus status = executor.getStatus();

        // then
        assertThat(status.getMaxPermits()).isEqualTo(3);
        assertThat(status.getInFlightCount()).isEqualTo(2);
        assertThat(status.getAvailablePermits()).isEqualTo(1); // 3 - 2 = 1

        // cleanup
        blockLatch.countDown();
    }

    @Test
    @DisplayName("adjustMaxPermits()로 동적으로 동시 처리 개수 조절")
    void adjustMaxPermits_DynamicallyChangesCapacity() throws InterruptedException {
        // given
        BlockingThreadPoolExecutor.BackpressureStatus beforeStatus = executor.getStatus();
        assertThat(beforeStatus.getMaxPermits()).isEqualTo(3);

        // when - maxPermits를 5로 증가
        executor.adjustMaxPermits(5);

        // then
        BlockingThreadPoolExecutor.BackpressureStatus afterStatus = executor.getStatus();
        assertThat(afterStatus.getMaxPermits()).isEqualTo(5);
        assertThat(afterStatus.getAvailablePermits()).isEqualTo(5);
    }

    @Test
    @DisplayName("예외가 발생해도 Semaphore가 정상적으로 해제된다")
    void execute_ReleasesSemaphore_EvenOnException() throws InterruptedException {
        // given
        CountDownLatch exceptionTaskDone = new CountDownLatch(1);

        // 예외를 던지는 작업 실행
        try {
            executor.execute(() -> {
                exceptionTaskDone.countDown();
                throw new RuntimeException("테스트 예외");
            });
        } catch (Exception ignored) {
            // executor 내부에서 예외 처리됨
        }

        exceptionTaskDone.await(2, TimeUnit.SECONDS);
        Thread.sleep(100); // Semaphore 해제 시간

        // when - 새 작업이 정상적으로 수락되어야 함
        BlockingThreadPoolExecutor.BackpressureStatus status = executor.getStatus();

        // then - Semaphore가 해제되어 availablePermits가 3이어야 함
        assertThat(status.getAvailablePermits()).isEqualTo(3);
    }

    @Test
    @DisplayName("스레드 인터럽트 시 RejectedExecutionException 발생")
    void execute_ThrowsRejectedExecutionException_WhenInterrupted() throws InterruptedException {
        // given - Semaphore를 가득 채움
        CountDownLatch blockLatch = new CountDownLatch(1);
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    blockLatch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(100);

        // when - 새 스레드에서 실행 후 인터럽트
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            assertThatThrownBy(() -> executor.execute(() -> {}))
                    .isInstanceOf(RejectedExecutionException.class);
        });

        testThread.start();
        testThread.join(2000);

        // cleanup
        blockLatch.countDown();
    }
}
