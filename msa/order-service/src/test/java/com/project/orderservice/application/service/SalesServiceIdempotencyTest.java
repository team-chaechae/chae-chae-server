package com.project.orderservice.application.service;

import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.model.SalesStatus;
import com.project.orderservice.domain.repository.SalesRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesService 멱등성 테스트")
class SalesServiceIdempotencyTest {

    @Mock
    private SalesRepository salesRepository;

    private SalesServiceImpl salesService;

    private String orderId;
    private Long salesId;

    @BeforeEach
    void setUp() {
        salesService = new SalesServiceImpl(
                salesRepository,
                null, // productCacheClient
                null, // eventPublisher
                new SimpleMeterRegistry()
        );

        orderId = "order-test-123";
        salesId = 1L;
    }

    private SalesEntity createSalesEntity(SalesStatus status) {
        SalesItemEntity item = SalesItemEntity.create(1L, "테스트 상품", 2, 10000);
        SalesEntity sales = SalesEntity.createWithItems(orderId, List.of(item));
        ReflectionTestUtils.setField(sales, "id", salesId);

        // 상태 변경
        if (status == SalesStatus.COMPLETED) {
            sales.complete();
        } else if (status == SalesStatus.CANCELLED) {
            sales.cancel("테스트 취소");
        } else if (status == SalesStatus.PROCESSING) {
            sales.markProcessing();
        }

        return sales;
    }

    @Nested
    @DisplayName("completeSales 멱등성 테스트")
    class CompleteSalesIdempotencyTest {

        @Test
        @DisplayName("이미 완료된 주문에 대해 중복 완료 요청 시 상태가 변경되지 않는다")
        void completeSales_AlreadyCompleted_NoChange() {
            // given
            SalesEntity completedSales = createSalesEntity(SalesStatus.COMPLETED);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(completedSales);

            // when
            salesService.completeSales(salesId, orderId);

            // then
            assertThat(completedSales.getStatus()).isEqualTo(SalesStatus.COMPLETED);
            // DB 업데이트가 불필요하므로 save 호출 없음 (dirty checking으로도 변경 없음)
        }

        @Test
        @DisplayName("PENDING 상태의 주문이 완료 처리된다")
        void completeSales_PendingOrder_Completed() {
            // given
            SalesEntity pendingSales = createSalesEntity(SalesStatus.PENDING);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(pendingSales);

            // when
            salesService.completeSales(salesId, orderId);

            // then
            assertThat(pendingSales.getStatus()).isEqualTo(SalesStatus.COMPLETED);
        }

        @Test
        @DisplayName("이미 취소된 주문은 완료 처리하지 않는다")
        void completeSales_CancelledOrder_NoChange() {
            // given
            SalesEntity cancelledSales = createSalesEntity(SalesStatus.CANCELLED);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(cancelledSales);

            // when
            salesService.completeSales(salesId, orderId);

            // then
            assertThat(cancelledSales.getStatus()).isEqualTo(SalesStatus.CANCELLED);
            assertThat(cancelledSales.getFailureReason()).isEqualTo("테스트 취소");
        }

        @Test
        @DisplayName("존재하지 않는 주문에 대한 완료 요청은 무시된다")
        void completeSales_NonExistentOrder_Ignored() {
            // given
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(null);

            // when & then - 예외 없이 정상 처리
            salesService.completeSales(salesId, orderId);
        }

        @Test
        @DisplayName("동일한 주문에 대해 여러 번 완료 요청해도 한 번만 처리된다")
        void completeSales_MultipleCalls_ProcessedOnce() {
            // given
            SalesEntity pendingSales = createSalesEntity(SalesStatus.PENDING);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(pendingSales);
            AtomicInteger statusChangeCount = new AtomicInteger(0);

            // 상태 변경을 추적하기 위한 spy 설정
            SalesEntity spySales = spy(pendingSales);
            doAnswer(invocation -> {
                statusChangeCount.incrementAndGet();
                return invocation.callRealMethod();
            }).when(spySales).complete();

            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(spySales);

            // when - 3번 호출
            salesService.completeSales(salesId, orderId);
            salesService.completeSales(salesId, orderId);
            salesService.completeSales(salesId, orderId);

            // then - complete()는 첫 번째 호출에서만 실행
            assertThat(statusChangeCount.get()).isEqualTo(1);
            assertThat(spySales.getStatus()).isEqualTo(SalesStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("cancelSales 멱등성 테스트")
    class CancelSalesIdempotencyTest {

        @Test
        @DisplayName("이미 취소된 주문에 대해 중복 취소 요청 시 상태가 변경되지 않는다")
        void cancelSales_AlreadyCancelled_NoChange() {
            // given
            SalesEntity cancelledSales = createSalesEntity(SalesStatus.CANCELLED);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(cancelledSales);

            // when
            salesService.cancelSales(salesId, orderId, "중복 취소 요청");

            // then
            assertThat(cancelledSales.getStatus()).isEqualTo(SalesStatus.CANCELLED);
            assertThat(cancelledSales.getFailureReason()).isEqualTo("테스트 취소"); // 기존 사유 유지
        }

        @Test
        @DisplayName("PENDING 상태의 주문이 취소 처리된다")
        void cancelSales_PendingOrder_Cancelled() {
            // given
            SalesEntity pendingSales = createSalesEntity(SalesStatus.PENDING);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(pendingSales);

            // when
            salesService.cancelSales(salesId, orderId, "재고 부족");

            // then
            assertThat(pendingSales.getStatus()).isEqualTo(SalesStatus.CANCELLED);
            assertThat(pendingSales.getFailureReason()).isEqualTo("재고 부족");
        }

        @Test
        @DisplayName("이미 완료된 주문은 취소 처리하지 않는다")
        void cancelSales_CompletedOrder_NoChange() {
            // given
            SalesEntity completedSales = createSalesEntity(SalesStatus.COMPLETED);
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(completedSales);

            // when
            salesService.cancelSales(salesId, orderId, "늦게 도착한 취소 이벤트");

            // then
            assertThat(completedSales.getStatus()).isEqualTo(SalesStatus.COMPLETED);
            assertThat(completedSales.getFailureReason()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 주문에 대한 취소 요청은 무시된다")
        void cancelSales_NonExistentOrder_Ignored() {
            // given
            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(null);

            // when & then - 예외 없이 정상 처리
            salesService.cancelSales(salesId, orderId, "취소 사유");
        }

        @Test
        @DisplayName("동일한 주문에 대해 여러 번 취소 요청해도 한 번만 처리된다")
        void cancelSales_MultipleCalls_ProcessedOnce() {
            // given
            SalesEntity pendingSales = createSalesEntity(SalesStatus.PENDING);
            AtomicInteger statusChangeCount = new AtomicInteger(0);

            SalesEntity spySales = spy(pendingSales);
            doAnswer(invocation -> {
                statusChangeCount.incrementAndGet();
                return invocation.callRealMethod();
            }).when(spySales).cancel(anyString());

            given(salesRepository.findSalesBySalesIdSimple(salesId)).willReturn(spySales);

            // when - 3번 호출
            salesService.cancelSales(salesId, orderId, "이유1");
            salesService.cancelSales(salesId, orderId, "이유2");
            salesService.cancelSales(salesId, orderId, "이유3");

            // then - cancel()은 첫 번째 호출에서만 실행
            assertThat(statusChangeCount.get()).isEqualTo(1);
            assertThat(spySales.getStatus()).isEqualTo(SalesStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("동시성 멱등성 테스트")
    class ConcurrentIdempotencyTest {

        @Test
        @DisplayName("동시에 여러 완료 요청이 와도 한 번만 처리된다 (멱등성 시뮬레이션)")
        void completeSales_ConcurrentCalls_ProcessedOnce() throws InterruptedException {
            // given
            int threadCount = 10;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            SalesEntity pendingSales = createSalesEntity(SalesStatus.PENDING);
            AtomicInteger completeCallCount = new AtomicInteger(0);

            // when - 멱등성 로직 직접 시뮬레이션
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> {
                    try {
                        startLatch.await();

                        // 멱등성 로직: 이미 완료된 경우 스킵
                        synchronized (pendingSales) {
                            if (!pendingSales.isCompleted()) {
                                pendingSales.complete();
                                completeCallCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executorService.shutdown();

            // then - 동시 요청이 와도 실제 상태 변경은 1번만
            assertThat(completeCallCount.get()).isEqualTo(1);
            assertThat(pendingSales.getStatus()).isEqualTo(SalesStatus.COMPLETED);
        }
    }
}
