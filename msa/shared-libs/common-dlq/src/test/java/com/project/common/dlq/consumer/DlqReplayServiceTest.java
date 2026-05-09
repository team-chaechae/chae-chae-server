package com.project.common.dlq.consumer;

import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqRecord;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.common.dlq.domain.DlqStatus;
import com.project.common.dlq.producer.DlqMessageProducer;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("DlqReplayService")
class DlqReplayServiceTest {

    @Test
    @DisplayName("Kafka 재발행 실패 시 DLQ 레코드를 RESOLVED로 표시하지 않는다")
    void replayRecord_publishFails_keepsRecordPending() {
        DlqRecordRepository repository = mock(DlqRecordRepository.class);
        DlqMessageProducer producer = mock(DlqMessageProducer.class);
        DlqReplayService service = new DlqReplayService(repository, producer, new DlqProperties());

        DlqRecord record = DlqRecord.builder()
                .originalTopic("order-created")
                .originalKey("order-1")
                .originalPayload("{\"orderId\":\"order-1\"}")
                .status(DlqStatus.PENDING)
                .retryCount(0)
                .serviceName("payment-service")
                .build();
        setId(record, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(record));
        when(producer.replayToOriginalSync(any())).thenReturn(false);

        assertThatThrownBy(() -> service.replayRecord(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("리플레이 실패");

        assertThat(record.getStatus()).isEqualTo(DlqStatus.PENDING);
        assertThat(record.getResolvedAt()).isNull();
        assertThat(record.getRetryCount()).isEqualTo(1);
    }

    private static void setId(DlqRecord record, Long id) {
        try {
            Field field = DlqRecord.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(record, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
