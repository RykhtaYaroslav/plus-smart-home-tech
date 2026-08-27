package ru.yandex.practicum.telemetry.collector.kafka;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public interface EventKafkaProducer {
    void send(SensorEventAvro event);

    void send(HubEventAvro event);
}
