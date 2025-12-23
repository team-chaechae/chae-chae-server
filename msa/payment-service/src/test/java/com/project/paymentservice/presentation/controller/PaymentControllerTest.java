package com.project.paymentservice.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.infrastructure.repository.JpaPaymentRepository;
import com.project.paymentservice.presentation.request.ReqPaymentDTO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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
