package com.project.orderservice.application.event.listener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompletableFuture 콜백 트랜잭션 경계")
class CompletableFutureTransactionBoundaryTest {

    @Test
    @DisplayName("트랜잭션 중 등록한 whenComplete 콜백도 트랜잭션 종료 후 실행되면 트랜잭션 밖에서 동작한다")
    void whenCompleteRegisteredInTransaction_ExecutedAfterTransactionEnd_RunsOutsideTransaction() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicBoolean transactionActiveWhenRegistered = new AtomicBoolean();
        AtomicBoolean transactionActiveInCallback = new AtomicBoolean(true);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            transactionActiveWhenRegistered.set(TransactionSynchronizationManager.isActualTransactionActive());

            future.whenComplete((result, exception) ->
                    transactionActiveInCallback.set(TransactionSynchronizationManager.isActualTransactionActive())
            );
        } finally {
            TransactionSynchronizationManager.clear();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }

        future.complete(null);

        assertThat(transactionActiveWhenRegistered).isTrue();
        assertThat(transactionActiveInCallback).isFalse();
    }
}
