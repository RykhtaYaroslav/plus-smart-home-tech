package ru.yandex.practicum.telemetry.collector.handler.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto.PayloadCase;
import ru.yandex.practicum.telemetry.collector.model.mapper.sensor.MotionSensorEventMapper;
import ru.yandex.practicum.telemetry.collector.service.CollectorService;

@Component
@RequiredArgsConstructor
public class MotionSensorEventHandler implements SensorEventHandler {
    private final CollectorService service;
    private final MotionSensorEventMapper mapper;

    @Override
    public PayloadCase getMessageType() {
        return PayloadCase.MOTION_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        service.collect(mapper.mapToModel(event));
    }
}
