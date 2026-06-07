package com.project.paymentservice.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.infrastructure.order.OrderSalesClient;
import com.project.paymentservice.infrastructure.order.dto.OrderSalesResponse;
import com.project.paymentservice.infrastructure.tosspayments.TossPaymentClient;
import com.project.paymentservice.infrastructure.tosspayments.TossPaymentException;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentCancelResponse;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentConfirmResponse;
import com.project.paymentservice.infrastructure.repository.JpaPaymentRepository;
import com.project.paymentservice.presentation.request.ReqPaymentDTO;
import com.project.paymentservice.presentation.request.ReqTossPaymentCancelDTO;
import com.project.paymentservice.presentation.request.ReqTossPaymentConfirmDTO;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "toss.payments.client-key=test_ck_local")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment API 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaPaymentRepository paymentRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private TossPaymentClient tossPaymentClient;

    @MockBean
    private OrderSalesClient orderSalesClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/payment - 결제 생성 성공")
    void processPayment_Success() throws Exception {
        // given
        ReqPaymentDTO request = new ReqPaymentDTO("order-1", 1L, 50000);

        // when & then
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.salesId").value(1))
                .andExpect(jsonPath("$.payment.amount").value(50000))
                .andExpect(jsonPath("$.payment.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/payment - 중복 결제 요청 시 기존 결제 반환")
    void processPayment_Duplicate_ReturnsExisting() throws Exception {
        // given
        ReqPaymentDTO request = new ReqPaymentDTO("order-2", 2L, 30000);

        // 첫 번째 결제
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when - 두 번째 결제 (중복)
        MvcResult result = mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.salesId").value(2))
                .andExpect(jsonPath("$.payment.amount").value(30000))
                .andReturn();

        // then - DB에는 하나만 저장
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/payment - 필수 필드 누락 시 400 에러")
    void processPayment_MissingField_BadRequest() throws Exception {
        // given - salesId 누락
        String invalidRequest = "{\"amount\": 50000}";

        // when & then
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payment - 금액이 0 이하면 400 에러")
    void processPayment_InvalidAmount_BadRequest() throws Exception {
        // given
        ReqPaymentDTO request = new ReqPaymentDTO("order-3", 3L, -1000);

        // when & then
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payment/toss/confirm - 토스페이먼츠 결제 승인 성공")
    void confirmTossPayment_Success() throws Exception {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        ReqTossPaymentConfirmDTO request = new ReqTossPaymentConfirmDTO(paymentKey, "order-7", 7L, 45000);
        given(orderSalesClient.getSales(7L)).willReturn(new OrderSalesResponse.SalesDetail(
                7L,
                "order-7",
                "PENDING",
                List.of(new OrderSalesResponse.SalesItemDetail(1L, 1999L, "상품_1999", 1, 45000, 45000)),
                1,
                45000
        ));
        given(tossPaymentClient.confirmPayment(paymentKey, "order-7", 45000, "payment-confirm-7"))
                .willReturn(new TossPaymentConfirmResponse(
                        paymentKey,
                        "order-7",
                        "DONE",
                        45000,
                        "카드",
                        OffsetDateTime.now()
                ));

        // when & then
        mockMvc.perform(post("/api/payment/toss/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.salesId").value(7))
                .andExpect(jsonPath("$.payment.amount").value(45000))
                .andExpect(jsonPath("$.payment.status").value("COMPLETED"))
                .andExpect(jsonPath("$.payment.tossPaymentKey").value(paymentKey))
                .andExpect(jsonPath("$.payment.paymentMethod").value("카드"));
    }

    @Test
    @DisplayName("POST /api/payment/toss/confirm - 토스페이먼츠 결제 승인 실패는 전역 핸들러가 응답한다")
    void confirmTossPayment_TossFailure_ReturnsGlobalErrorResponse() throws Exception {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        ReqTossPaymentConfirmDTO request = new ReqTossPaymentConfirmDTO(paymentKey, "order-17", 17L, 45000);
        given(orderSalesClient.getSales(17L)).willReturn(new OrderSalesResponse.SalesDetail(
                17L,
                "order-17",
                "PENDING",
                List.of(new OrderSalesResponse.SalesItemDetail(1L, 1999L, "상품_1999", 1, 45000, 45000)),
                1,
                45000
        ));
        given(tossPaymentClient.confirmPayment(paymentKey, "order-17", 45000, "payment-confirm-17"))
                .willThrow(new TossPaymentException("REJECT_CARD_COMPANY", "카드사 승인 거절", 400));

        // when & then
        mockMvc.perform(post("/api/payment/toss/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("카드사 승인 거절"))
                .andExpect(jsonPath("$.code").value("REJECT_CARD_COMPANY"));
    }

    @Test
    @DisplayName("POST /api/payment/toss/cancel - 토스페이먼츠 결제 취소 성공")
    void cancelTossPayment_Success() throws Exception {
        // given
        String paymentKey = "tgen_20260519123456AbCdE";
        ReqTossPaymentConfirmDTO confirmRequest = new ReqTossPaymentConfirmDTO(paymentKey, "order-8", 8L, 45000);
        given(orderSalesClient.getSales(8L)).willReturn(new OrderSalesResponse.SalesDetail(
                8L,
                "order-8",
                "PENDING",
                List.of(new OrderSalesResponse.SalesItemDetail(1L, 1999L, "상품_1999", 1, 45000, 45000)),
                1,
                45000
        ));
        given(tossPaymentClient.confirmPayment(paymentKey, "order-8", 45000, "payment-confirm-8"))
                .willReturn(new TossPaymentConfirmResponse(
                        paymentKey,
                        "order-8",
                        "DONE",
                        45000,
                        "카드",
                        OffsetDateTime.now()
                ));

        mockMvc.perform(post("/api/payment/toss/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        ReqTossPaymentCancelDTO cancelRequest = new ReqTossPaymentCancelDTO(8L, "고객 요청");
        given(tossPaymentClient.cancelPayment(paymentKey, "고객 요청", "payment-cancel-8"))
                .willReturn(new TossPaymentCancelResponse(
                        paymentKey,
                        "order-8",
                        "CANCELED",
                        45000,
                        "카드",
                        OffsetDateTime.now(),
                        List.of(new TossPaymentCancelResponse.CancelDetail(
                                45000,
                                "고객 요청",
                                0,
                                OffsetDateTime.now(),
                                "cancel_tx_key",
                                "DONE"
                        ))
                ));

        // when & then
        mockMvc.perform(post("/api/payment/toss/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.salesId").value(8))
                .andExpect(jsonPath("$.payment.status").value("REFUNDED"))
                .andExpect(jsonPath("$.payment.tossPaymentKey").value(paymentKey));
    }

    @Test
    @DisplayName("GET /api/payment/toss/config - 토스페이먼츠 클라이언트 키 조회 성공")
    void getTossPaymentConfig_Success() throws Exception {
        mockMvc.perform(get("/api/payment/toss/config"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientKey").value("test_ck_local"));
    }

    @Test
    @DisplayName("GET /api/payment/sales/{salesId} - 결제 조회 성공")
    void getPaymentBySalesId_Success() throws Exception {
        // given - 먼저 결제 생성
        ReqPaymentDTO request = new ReqPaymentDTO("order-4", 4L, 25000);
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/api/payment/sales/4"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payment.salesId").value(4))
                .andExpect(jsonPath("$.payment.amount").value(25000))
                .andExpect(jsonPath("$.payment.histories").isArray());
    }

    @Test
    @DisplayName("GET /api/payment/sales/{salesId} - 존재하지 않는 결제 조회 시 404 에러")
    void getPaymentBySalesId_NotFound() throws Exception {
        // when & then
        mockMvc.perform(get("/api/payment/sales/9999"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/payment/sales/{salesId}/status - 결제 상태 조회 성공")
    void getPaymentStatus_Success() throws Exception {
        // given
        ReqPaymentDTO request = new ReqPaymentDTO("order-5", 5L, 15000);
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/api/payment/sales/5/status"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salesId").value(5))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/payment/sales/{salesId}/refund - 환불 성공")
    void refundPayment_Success() throws Exception {
        // given - 먼저 결제 생성
        ReqPaymentDTO request = new ReqPaymentDTO("order-6", 6L, 20000);
        mockMvc.perform(post("/api/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/api/payment/sales/6/refund")
                        .param("reason", "고객 요청"))
                .andDo(print())
                .andExpect(status().isOk());

        // 환불 후 상태 확인
        mockMvc.perform(get("/api/payment/sales/6/status"))
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
