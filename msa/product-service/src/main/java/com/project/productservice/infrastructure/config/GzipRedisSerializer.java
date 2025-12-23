package com.project.productservice.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Gzip 압축 Redis Serializer
 * JSON 직렬화 후 Gzip 압축하여 저장
 */
@Slf4j
public class GzipRedisSerializer<T> implements RedisSerializer<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public GzipRedisSerializer(Class<T> type) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.type = type;
    }

    @Override
    public byte[] serialize(T value) throws SerializationException {
        if (value == null) {
            return null;
        }

        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(value);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(jsonBytes);
            }

            byte[] compressed = baos.toByteArray();
            log.debug("Gzip 압축: {} bytes → {} bytes ({}% 감소)",
                jsonBytes.length, compressed.length,
                Math.round((1 - (double) compressed.length / jsonBytes.length) * 100));

            return compressed;
        } catch (Exception e) {
            throw new SerializationException("Gzip 직렬화 실패", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (GZIPInputStream gzip = new GZIPInputStream(bais)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzip.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }

            return objectMapper.readValue(baos.toByteArray(), type);
        } catch (Exception e) {
            throw new SerializationException("Gzip 역직렬화 실패", e);
        }
    }
}
