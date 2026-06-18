package com.project.inventoryservice.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcInventoryRepository")
class JdbcInventoryRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("confirmByOrderId는 예약 row를 CONFIRMED로 변경한다")
    void confirmByOrderId_UpdatesReservedRowsToConfirmed() {
        JdbcInventoryRepository repository = new JdbcInventoryRepository(jdbcTemplate);
        given(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq("order-7"))).willReturn(1);

        int updated = repository.confirmByOrderId("order-7");

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq("order-7"));
        assertThat(sqlCaptor.getValue()).contains("UPDATE inventory");
        assertThat(sqlCaptor.getValue()).contains("status = 'CONFIRMED'");
        assertThat(sqlCaptor.getValue()).contains("change_type = 'ORDER_DECREASE'");
        assertThat(sqlCaptor.getValue()).contains("status = 'RESERVED'");
    }

    @Test
    @DisplayName("cancelByOrderId는 예약 row를 CANCELLED로 변경한다")
    void cancelByOrderId_UpdatesReservedRowsToCancelled() {
        JdbcInventoryRepository repository = new JdbcInventoryRepository(jdbcTemplate);
        given(jdbcTemplate.update(org.mockito.ArgumentMatchers.anyString(), eq("order-7"))).willReturn(1);

        int updated = repository.cancelByOrderId("order-7");

        assertThat(updated).isEqualTo(1);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq("order-7"));
        assertThat(sqlCaptor.getValue()).contains("UPDATE inventory");
        assertThat(sqlCaptor.getValue()).contains("status = 'CANCELLED'");
        assertThat(sqlCaptor.getValue()).contains("change_type = 'ORDER_DECREASE'");
        assertThat(sqlCaptor.getValue()).contains("status = 'RESERVED'");
    }
}
