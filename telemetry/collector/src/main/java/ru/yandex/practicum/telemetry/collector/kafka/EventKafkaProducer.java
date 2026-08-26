package ru.yandex.practicum.telemetry.collector.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
@RequiredArgsConstructor
public class EventKafkaProducer {
    @Value("${collector.kafka.sensor-topic}")
    private final String sensorTopic;

    @Value("${collector.kafka.hub-topic}")
    private final String hubTopic;

    private final KafkaProducer<String, SpecificRecordBase> producer;

    public void sendSensorEvent(SensorEventAvro event) {
        ProducerRecord<String, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(
                        sensorTopic,
                        event.getHubId(),
                        event
                );

        producer.send(producerRecord);
    }

    public void sendHubEvent(HubEventAvro event) {
        ProducerRecord<String, SpecificRecordBase> producerRecord =
                new ProducerRecord<>(
                        hubTopic,
                        event.getHubId(),
                        event
                );

        producer.send(producerRecord);
    }
}
