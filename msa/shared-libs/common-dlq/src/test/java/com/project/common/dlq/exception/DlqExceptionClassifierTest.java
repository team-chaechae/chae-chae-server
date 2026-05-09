package com.project.common.dlq.exception;

import com.project.common.dlq.domain.DlqMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DlqExceptionClassifier")
class DlqExceptionClassifierTest {

    @Test
    @DisplayName("BusinessException은 BUSINESS로 분류된다")
    void classify_businessException() {
        DlqExceptionClassifier classifier = new DlqExceptionClassifier();
        DlqMessage.ExceptionCategory category = classifier.classify(
                new BusinessException(TestErrorCode.FAIL, "비즈니스 실패")
        );
        assertThat(category).isEqualTo(DlqMessage.ExceptionCategory.BUSINESS);
    }

    private enum TestErrorCode implements BaseErrorCode {
        FAIL;

        @Override
        public HttpStatus getHttpStatus() {
            return HttpStatus.BAD_REQUEST;
        }

        @Override
        public String getCode() {
            return "FAIL";
        }

        @Override
        public String getMessage() {
            return "실패";
        }
    }
}
