package ru.yandex.practicum.telemetry.collector.model.mapper;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;

public interface SensorEventAvroMapper {
    SensorEventAvro map(SensorEvent event);
}
