package ru.yandex.practicum.telemetry.collector.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent;

import java.time.Instant;

@Component
public class TemperatureSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventType getMessageType() {
        return SensorEventType.TEMPERATURE_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro mapToAvro(SensorEvent event) {
        TemperatureSensorEvent temperature = (TemperatureSensorEvent) event;

        return SensorEventAvro.newBuilder()
                .setId(temperature.getId())
                .setHubId(temperature.getHubId())
                .setTimestamp(temperature.getTimestamp())
                .setPayload(TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(temperature.getTemperatureC())
                        .setTemperatureF(temperature.getTemperatureF())
                        .build())
                .build();
    }

    @Override
    public TemperatureSensorEvent mapToModel(SensorEventProto event) {
        TemperatureSensorProto payload = event.getTemperatureSensor();
        TemperatureSensorEvent temperature = new TemperatureSensorEvent();
        temperature.setId(event.getId());
        temperature.setHubId(event.getHubId());
        temperature.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        temperature.setTemperatureC(payload.getTemperatureC());
        temperature.setTemperatureF(payload.getTemperatureF());
        return temperature;
    }
}
