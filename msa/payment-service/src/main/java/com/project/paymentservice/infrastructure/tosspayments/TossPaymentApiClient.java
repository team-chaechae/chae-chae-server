package com.project.paymentservice.infrastructure.tosspayments;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentCancelRequest;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentCancelResponse;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentConfirmRequest;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentConfirmResponse;
import com.project.paymentservice.infrastructure.tosspayments.dto.TossPaymentErrorResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class TossPaymentApiClient implements TossPaymentClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String CANCEL_PATH = "/v1/payments/{paymentKey}/cancel";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final RestClient tossPaymentRestClient;
    private final TossPaymentProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public TossPaymentConfirmResponse confirmPayment(
            String paymentKey,
            String orderId,
            Integer amount,
            String idempotencyKey
    ) {
        validateSecretKey();

        try {
            return tossPaymentRestClient.post()
                    .uri(CONFIRM_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                    .body(new TossPaymentConfirmRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(TossPaymentConfirmResponse.class);
        } catch (RestClientResponseException e) {
            throw toTossPaymentException(e);
        } catch (RestClientException e) {
            throw new TossPaymentException(
                    "TOSS_PAYMENT_REQUEST_FAILED",
                    "토스페이먼츠 결제 승인 요청에 실패했습니다.",
                    500,
                    e
            );
        }
    }

    @Override
    public TossPaymentCancelResponse cancelPayment(
            String paymentKey,
            String cancelReason,
            String idempotencyKey
    ) {
        validateSecretKey();

        try {
            return tossPaymentRestClient.post()
                    .uri(CANCEL_PATH, paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                    .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                    .body(new TossPaymentCancelRequest(cancelReason))
                    .retrieve()
                    .body(TossPaymentCancelResponse.class);
        } catch (RestClientResponseException e) {
            throw toTossPaymentException(e);
        } catch (RestClientException e) {
            throw new TossPaymentException(
                    "TOSS_PAYMENT_REQUEST_FAILED",
                    "토스페이먼츠 결제 취소 요청에 실패했습니다.",
                    500,
                    e
            );
        }
    }

    private void validateSecretKey() {
        if (!StringUtils.hasText(properties.getSecretKey())) {
            throw new TossPaymentException(
                    "TOSS_PAYMENT_SECRET_KEY_MISSING",
                    "토스페이먼츠 시크릿 키가 설정되어 있지 않습니다.",
                    500
            );
        }
    }

    private String authorizationHeader() {
        String credentials = properties.getSecretKey() + ":";
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    private TossPaymentException toTossPaymentException(RestClientResponseException e) {
        TossPaymentErrorResponse error = parseErrorResponse(e.getResponseBodyAsString(StandardCharsets.UTF_8));
        String code = StringUtils.hasText(error.code()) ? error.code() : "TOSS_PAYMENT_ERROR";
        String message = StringUtils.hasText(error.message()) ? error.message() : "토스페이먼츠 요청에 실패했습니다.";
        return new TossPaymentException(code, message, e.getStatusCode().value());
    }

    private TossPaymentErrorResponse parseErrorResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return new TossPaymentErrorResponse(null, null);
        }

        try {
            return objectMapper.readValue(responseBody, TossPaymentErrorResponse.class);
        } catch (JsonProcessingException e) {
            return new TossPaymentErrorResponse(null, null);
        }
    }
}
