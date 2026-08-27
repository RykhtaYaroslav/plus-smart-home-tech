package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.telemetry.mapper.AvroSerializer;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {
    @Value("${collector.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaProducer<String, SpecificRecordBase> kafkaProducer() {
        Properties properties = new Properties();

        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                AvroSerializer.class
        );

        return new KafkaProducer<>(properties);
    }
}
