package ru.yandex.practicum.telemetry.aggregator.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.mapper.AvroSerializer;

import java.util.Properties;

@Configuration
public class KafkaProducerConfig {
    @Value("${aggregator.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${aggregator.kafka.producer.acks}")
    private String acks;

    @Value("${aggregator.kafka.producer.retries}")
    private int retries;

    @Value("${aggregator.kafka.producer.enable-idempotence}")
    private boolean enableIdempotence;

    @Value("${aggregator.kafka.producer.delivery-timeout-ms}")
    private int deliveryTimeoutMs;

    @Value("${aggregator.kafka.producer.request-timeout-ms}")
    private int requestTimeoutMs;

    @Value("${aggregator.kafka.producer.linger-ms}")
    private int lingerMs;

    @Value("${aggregator.kafka.producer.batch-size}")
    private int batchSize;

    @Value("${aggregator.kafka.producer.buffer-memory}")
    private long bufferMemory;

    @Bean
    public KafkaProducer<String, SensorsSnapshotAvro> kafkaProducer() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, acks);
        properties.put(ProducerConfig.RETRIES_CONFIG, retries);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);

        return new KafkaProducer<>(properties);
    }
}
