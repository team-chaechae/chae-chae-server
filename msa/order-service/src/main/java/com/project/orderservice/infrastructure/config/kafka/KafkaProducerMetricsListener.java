package com.project.orderservice.infrastructure.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaProducerMetricsListener<K, V> implements ProducerFactory.Listener<K, V> {

    private final MeterRegistry meterRegistry;
    private final Map<String, KafkaClientMetrics> metricsById = new ConcurrentHashMap<>();

    public KafkaProducerMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void producerAdded(String id, Producer<K, V> producer) {
        KafkaClientMetrics metrics = new KafkaClientMetrics(producer);
        metrics.bindTo(meterRegistry);
        metricsById.put(id, metrics);
    }

    @Override
    public void producerRemoved(String id, Producer<K, V> producer) {
        KafkaClientMetrics metrics = metricsById.remove(id);
        if (metrics != null) {
            metrics.close();
        }
    }
}
