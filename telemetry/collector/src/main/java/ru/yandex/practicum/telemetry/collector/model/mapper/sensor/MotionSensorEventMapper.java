package ru.yandex.practicum.telemetry.collector.model.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;

import java.time.Instant;

@Component
public class MotionSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventType getMessageType() {
        return SensorEventType.MOTION_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro mapToAvro(SensorEvent event) {
        MotionSensorEvent motion = (MotionSensorEvent) event;

        return SensorEventAvro.newBuilder()
                .setId(motion.getId())
                .setHubId(motion.getHubId())
                .setTimestamp(motion.getTimestamp())
                .setPayload(MotionSensorAvro.newBuilder()
                        .setLinkQuality(motion.getLinkQuality())
                        .setMotion(motion.isMotion())
                        .setVoltage(motion.getVoltage())
                        .build())
                .build();
    }

    @Override
    public MotionSensorEvent mapToModel(SensorEventProto event) {
        MotionSensorProto payload = event.getMotionSensor();
        MotionSensorEvent motion = new MotionSensorEvent();
        motion.setId(event.getId());
        motion.setHubId(event.getHubId());
        motion.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        motion.setLinkQuality(payload.getLinkQuality());
        motion.setMotion(payload.getMotion());
        motion.setVoltage(payload.getVoltage());
        return motion;
    }
}
