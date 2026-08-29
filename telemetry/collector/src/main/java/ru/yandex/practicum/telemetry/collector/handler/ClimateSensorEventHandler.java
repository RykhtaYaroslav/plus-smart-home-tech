package ru.yandex.practicum.telemetry.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto.PayloadCase;

@Component
public class ClimateSensorEventHandler implements SensorEventHandler {
    @Override
    public PayloadCase getMessageType() {
        return PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
    }
}
