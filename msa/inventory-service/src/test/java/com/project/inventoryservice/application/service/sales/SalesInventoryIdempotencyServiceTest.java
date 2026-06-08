package com.project.inventoryservice.application.service.sales;

import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesInventoryIdempotencyService")
class SalesInventoryIdempotencyServiceTest {

    private static final String OPERATION_ID = "payment-orchestration:7:inventory-restore";
    private static final String COMPLETED_KEY = "inventory:sales:completed:" + OPERATION_ID;
    private static final String PROCESSING_KEY = "inventory:sales:processing:" + OPERATION_ID;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> completedBucket;

    @Mock
    private RBucket<String> processingBucket;

    @Test
    @DisplayName("이미 완료된 operationId이면 명령을 실행하지 않고 중복 성공 응답을 반환한다")
    void execute_WhenOperationCompleted_ReturnsDuplicateWithoutExecutingCommand() {
        // given
        SalesInventoryIdempotencyService service = service();
        givenCompletedBucket();
        given(completedBucket.isExists()).willReturn(true);

        // when
        ResSalesInventoryDTO response = service.execute(OPERATION_ID, () -> {
            throw new AssertionError("중복 완료 명령은 실행되면 안 됩니다.");
        });

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getProcessedCount()).isZero();
        verify(processingBucket, never()).setIfAbsent(any(), any(Duration.class));
    }

    @Test
    @DisplayName("신규 operationId이면 명령 실행 후 완료 키를 저장한다")
    void execute_WhenOperationNew_ExecutesCommandAndMarksCompleted() {
        // given
        SalesInventoryIdempotencyService service = service();
        givenBuckets();
        given(completedBucket.isExists()).willReturn(false);
        given(processingBucket.setIfAbsent(eq("PROCESSING"), any(Duration.class))).willReturn(true);

        // when
        ResSalesInventoryDTO response = service.execute(OPERATION_ID, () -> ResSalesInventoryDTO.success(List.of()));

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.isDuplicate()).isFalse();
        verify(completedBucket).set(eq("COMPLETED"), any(Duration.class));
        verify(processingBucket).delete();
    }

    @Test
    @DisplayName("같은 operationId가 처리 중이면 명령을 실행하지 않고 예외를 던진다")
    void execute_WhenOperationProcessing_ThrowsWithoutExecutingCommand() {
        // given
        SalesInventoryIdempotencyService service = service();
        givenBuckets();
        given(completedBucket.isExists()).willReturn(false);
        given(processingBucket.setIfAbsent(eq("PROCESSING"), any(Duration.class))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> service.execute(OPERATION_ID, () -> {
            throw new AssertionError("처리 중인 명령은 실행되면 안 됩니다.");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 처리 중인 재고 명령");
    }

    private SalesInventoryIdempotencyService service() {
        return new SalesInventoryIdempotencyService(redissonClient);
    }

    private void givenBuckets() {
        givenCompletedBucket();
        given(redissonClient.<String>getBucket(PROCESSING_KEY)).willReturn(processingBucket);
    }

    private void givenCompletedBucket() {
        given(redissonClient.<String>getBucket(COMPLETED_KEY)).willReturn(completedBucket);
    }
}
