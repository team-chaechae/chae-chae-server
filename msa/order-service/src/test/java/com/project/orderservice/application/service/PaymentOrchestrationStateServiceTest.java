package com.project.orderservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.project.orderservice.domain.model.PaymentOrchestrationEntity;
import com.project.orderservice.domain.model.PaymentOrchestrationStatus;
import com.project.orderservice.domain.repository.PaymentOrchestrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentOrchestrationStateService")
class PaymentOrchestrationStateServiceTest {

    @Mock
    private PaymentOrchestrationRepository orchestrationRepository;

    @Test
    @DisplayName("기존 오케스트레이션이 있으면 새로 생성하지 않고 반환한다")
    void getOrCreate_WhenExisting_ReturnsExisting() {
        // given
        PaymentOrchestrationStateService service = service();
        PaymentOrchestrationEntity existing = PaymentOrchestrationEntity.start("order-7", 7L);
        given(orchestrationRepository.findBySalesId(7L)).willReturn(Optional.of(existing));

        // when
        PaymentOrchestrationEntity result = service.getOrCreate("order-7", 7L);

        // then
        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("새 오케스트레이션 생성 중 unique 충돌이 나면 기존 row를 다시 조회한다")
    void getOrCreate_WhenCreateConflicts_LoadsExisting() {
        // given
        PaymentOrchestrationStateService service = service();
        PaymentOrchestrationEntity existing = PaymentOrchestrationEntity.start("order-7", 7L);
        given(orchestrationRepository.findBySalesId(7L))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existing));
        given(orchestrationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(PaymentOrchestrationEntity.class)))
                .willThrow(new DataIntegrityViolationException("duplicate sales_id"));

        // when
        PaymentOrchestrationEntity result = service.getOrCreate("order-7", 7L);

        // then
        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("재고 차감 완료 상태를 짧은 상태 트랜잭션으로 저장한다")
    void markInventoryDeducted_SavesState() {
        // given
        PaymentOrchestrationStateService service = service();
        PaymentOrchestrationEntity orchestration = PaymentOrchestrationEntity.start("order-7", 7L);
        given(orchestrationRepository.save(orchestration)).willReturn(orchestration);

        // when
        PaymentOrchestrationEntity result = service.markInventoryDeducted(orchestration);

        // then
        assertThat(result.getStatus()).isEqualTo(PaymentOrchestrationStatus.INVENTORY_DEDUCTED);
        assertThat(result.isInventoryDeducted()).isTrue();
        verify(orchestrationRepository).save(orchestration);
    }

    private PaymentOrchestrationStateService service() {
        return new PaymentOrchestrationStateService(orchestrationRepository);
    }
}
