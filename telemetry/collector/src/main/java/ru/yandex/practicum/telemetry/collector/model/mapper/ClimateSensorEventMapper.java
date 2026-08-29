package ru.yandex.practicum.telemetry.collector.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;

import java.time.Instant;

@Component
public class ClimateSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventType getMessageType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro mapToAvro(SensorEvent event) {
        ClimateSensorEvent climate = (ClimateSensorEvent) event;

        return SensorEventAvro.newBuilder()
                .setId(climate.getId())
                .setHubId(climate.getHubId())
                .setTimestamp(climate.getTimestamp())
                .setPayload(ClimateSensorAvro.newBuilder()
                        .setTemperatureC(climate.getTemperatureC())
                        .setHumidity(climate.getHumidity())
                        .setCo2Level(climate.getCo2Level())
                        .build())
                .build();
    }

    @Override
    public ClimateSensorEvent mapToModel(SensorEventProto event) {
        ClimateSensorProto payload = event.getClimateSensor();
        ClimateSensorEvent climate = new ClimateSensorEvent();
        climate.setId(event.getId());
        climate.setHubId(event.getHubId());
        climate.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        climate.setTemperatureC(payload.getTemperatureC());
        climate.setHumidity(payload.getHumidity());
        climate.setCo2Level(payload.getCo2Level());
        return climate;
    }
}
