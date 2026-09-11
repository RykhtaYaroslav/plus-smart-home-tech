package ru.yandex.practicum.telemetry.collector.handler.hub;

import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto.PayloadCase;

public interface HubEventHandler {
    PayloadCase getMessageType();

    void handle(HubEventProto event);
}
