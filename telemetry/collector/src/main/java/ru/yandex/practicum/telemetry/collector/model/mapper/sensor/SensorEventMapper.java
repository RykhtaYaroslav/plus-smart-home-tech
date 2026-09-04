package ru.yandex.practicum.telemetry.collector.model.mapper.sensor;

import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;

public interface SensorEventMapper {
    SensorEventType getMessageType();

    SensorEventAvro mapToAvro(SensorEvent event);

    SensorEvent mapToModel(SensorEventProto event);
}
