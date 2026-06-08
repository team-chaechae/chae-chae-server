package com.project.paymentservice.integration;

import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.application.service.PaymentService;
import com.project.paymentservice.domain.model.PaymentStatus;
import com.project.paymentservice.infrastructure.repository.JpaPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Payment 통합 테스트")
class PaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private JpaPaymentRepository paymentRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("결제 생성 후 DB에 정상 저장된다")
    void processPayment_SavesToDB() {
        // given
        String orderId = "order-1";
        Long salesId = 1L;
        Integer amount = 50000;

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result.getPayment().getSalesId()).isEqualTo(salesId);
        assertThat(result.getPayment().getAmount()).isEqualTo(amount);
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());

        // DB 확인
        var savedPayment = paymentRepository.findBySalesId(salesId);
        assertThat(savedPayment).isPresent();
        assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    @DisplayName("같은 salesId로 두 번 요청 시 중복 결제가 방지된다")
    void processPayment_PreventsDuplicatePayment() {
        // given
        String orderId = "order-2";
        Long salesId = 2L;
        Integer amount = 30000;

        // when - 첫 번째 결제
        ResPaymentDTO first = paymentService.processPayment(orderId, salesId, amount);

        // when - 두 번째 결제 (중복)
        ResPaymentDTO second = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(first.getPayment().getSalesId()).isEqualTo(salesId);
        assertThat(second.getPayment().getSalesId()).isEqualTo(salesId);

        // DB에는 하나만 저장되어야 함
        long count = paymentRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 같은 salesId로 요청해도 하나만 저장된다")
    void processPayment_ConcurrentRequests_OnlyOneSaved() throws InterruptedException {
        // given
        String orderId = "order-3";
        Long salesId = 3L;
        Integer amount = 10000;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateErrorCount = new AtomicInteger(0);

        // when - 10개 스레드가 동시에 같은 salesId로 결제 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    paymentService.processPayment(orderId, salesId, amount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Race condition으로 인한 unique 제약조건 위반 예외는 허용
                    // (2차 방어선 - DB unique constraint)
                    duplicateErrorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then - 성공 + 중복 에러 = 전체 요청 수
        assertThat(successCount.get() + duplicateErrorCount.get()).isEqualTo(threadCount);

        // 최소 1개는 성공해야 함
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        // DB에는 하나만 저장되어야 함 (핵심 검증)
        long count = paymentRepository.count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 salesId는 각각 저장된다")
    void processPayment_DifferentSalesIds_AllSaved() {
        // given & when
        paymentService.processPayment("order-10", 10L, 10000);
        paymentService.processPayment("order-20", 20L, 20000);
        paymentService.processPayment("order-30", 30L, 30000);

        // then
        assertThat(paymentRepository.count()).isEqualTo(3);
        assertThat(paymentRepository.findBySalesId(10L)).isPresent();
        assertThat(paymentRepository.findBySalesId(20L)).isPresent();
        assertThat(paymentRepository.findBySalesId(30L)).isPresent();
    }

    @Test
    @DisplayName("결제 조회 시 히스토리도 함께 조회된다")
    void getPaymentBySalesId_IncludesHistory() {
        // given
        Long salesId = 4L;
        paymentService.processPayment("order-4", salesId, 50000);

        // when
        ResPaymentDTO result = paymentService.getPaymentBySalesId(salesId);

        // then
        assertThat(result.getPayment().getHistories()).isNotEmpty();
        // 현재 결제 히스토리는 최종 상태만 기록한다.
        assertThat(result.getPayment().getHistories()).hasSize(1);
        assertThat(result.getPayment().getHistories().get(0).getStatus()).isEqualTo("COMPLETED");
    }
}
