package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class EventKafkaProducerImpl implements EventKafkaProducer, AutoCloseable {
    @Value("${collector.kafka.sensor-topic}")
    private String sensorTopic;

    @Value("${collector.kafka.hub-topic}")
    private String hubTopic;

    private final KafkaProducer<String, SpecificRecordBase> producer;

    @Override
    public void send(SensorEventAvro event) {
        ProducerRecord<String, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(
                        sensorTopic,
                        event.getHubId(),
                        event
                );

        producer.send(producerRecord);
        producer.flush();
    }

    @Override
    public void send(HubEventAvro event) {
        ProducerRecord<String, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(
                        hubTopic,
                        event.getHubId(),
                        event
                );

        producer.send(producerRecord);
        producer.flush();
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
