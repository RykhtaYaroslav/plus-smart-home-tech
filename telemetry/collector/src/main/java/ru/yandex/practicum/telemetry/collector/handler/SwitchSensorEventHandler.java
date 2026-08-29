package ru.yandex.practicum.telemetry.collector.handler;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto.PayloadCase;

@Component
public class SwitchSensorEventHandler implements SensorEventHandler {
    @Override
    public PayloadCase getMessageType() {
        return PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public void handle(SensorEventProto event) {
    }
}
