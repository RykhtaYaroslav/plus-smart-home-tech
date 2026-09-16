package ru.yandex.practicum.telemetry.collector.model.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent;

import java.time.Instant;

@Component
public class SwitchSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventType getMessageType() {
        return SensorEventType.SWITCH_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro mapToAvro(SensorEvent event) {
        SwitchSensorEvent switchEvent = (SwitchSensorEvent) event;

        return SensorEventAvro.newBuilder()
                .setId(switchEvent.getId())
                .setHubId(switchEvent.getHubId())
                .setTimestamp(switchEvent.getTimestamp())
                .setPayload(SwitchSensorAvro.newBuilder()
                        .setState(switchEvent.isState())
                        .build())
                .build();
    }

    @Override
    public SwitchSensorEvent mapToModel(SensorEventProto event) {
        SwitchSensorProto payload = event.getSwitchSensor();
        SwitchSensorEvent switchEvent = new SwitchSensorEvent();
        switchEvent.setId(event.getId());
        switchEvent.setHubId(event.getHubId());
        switchEvent.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        switchEvent.setState(payload.getState());
        return switchEvent;
    }
}
