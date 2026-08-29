package ru.yandex.practicum.telemetry.collector.model.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;

import java.time.Instant;

@Component
public class LightSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventType getMessageType() {
        return SensorEventType.LIGHT_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro mapToAvro(SensorEvent event) {
        LightSensorEvent light = (LightSensorEvent) event;

        return SensorEventAvro.newBuilder()
                .setId(light.getId())
                .setHubId(light.getHubId())
                .setTimestamp(light.getTimestamp())
                .setPayload(LightSensorAvro.newBuilder()
                        .setLinkQuality(light.getLinkQuality())
                        .setLuminosity(light.getLuminosity())
                        .build())
                .build();
    }

    @Override
    public LightSensorEvent mapToModel(SensorEventProto event) {
        LightSensorProto payload = event.getLightSensor();
        LightSensorEvent light = new LightSensorEvent();
        light.setId(event.getId());
        light.setHubId(event.getHubId());
        light.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        light.setLinkQuality(payload.getLinkQuality());
        light.setLuminosity(payload.getLuminosity());
        return light;
    }
}
