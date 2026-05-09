package com.project.orderservice.infrastructure.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaConsumerMetricsListener<K, V> implements ConsumerFactory.Listener<K, V> {

    private final MeterRegistry meterRegistry;
    private final Map<String, KafkaClientMetrics> metricsById = new ConcurrentHashMap<>();

    public KafkaConsumerMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void consumerAdded(String id, Consumer<K, V> consumer) {
        KafkaClientMetrics metrics = new KafkaClientMetrics(consumer);
        metrics.bindTo(meterRegistry);
        metricsById.put(id, metrics);
    }

    @Override
    public void consumerRemoved(String id, Consumer<K, V> consumer) {
        KafkaClientMetrics metrics = metricsById.remove(id);
        if (metrics != null) {
            metrics.close();
        }
    }
}
