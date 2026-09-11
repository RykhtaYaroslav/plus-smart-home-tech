package ru.yandex.practicum.telemetry.collector.handler.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto.PayloadCase;
import ru.yandex.practicum.telemetry.collector.model.mapper.sensor.SwitchSensorEventMapper;
import ru.yandex.practicum.telemetry.collector.service.CollectorService;

@Component
@RequiredArgsConstructor
public class SwitchSensorEventHandler implements SensorEventHandler {
    private final CollectorService service;
    private final SwitchSensorEventMapper mapper;

    @Override
    public PayloadCase getMessageType() {
        return PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
        service.collect(mapper.mapToModel(event));
    }
}
