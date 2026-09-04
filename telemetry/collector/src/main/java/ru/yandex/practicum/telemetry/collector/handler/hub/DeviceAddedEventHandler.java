package ru.yandex.practicum.telemetry.collector.handler.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto.PayloadCase;
import ru.yandex.practicum.telemetry.collector.model.mapper.hub.DeviceAddedEventMapper;
import ru.yandex.practicum.telemetry.collector.service.CollectorService;

@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {
    private final CollectorService service;
    private final DeviceAddedEventMapper mapper;

    @Override
    public PayloadCase getMessageType() {
        return PayloadCase.DEVICE_ADDED;
    }

    @Override
    public void handle(HubEventProto event) {
        service.collect(mapper.mapToModel(event));
    }
}
