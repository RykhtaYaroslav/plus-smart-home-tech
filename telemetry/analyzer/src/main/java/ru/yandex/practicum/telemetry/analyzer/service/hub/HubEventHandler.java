package ru.yandex.practicum.telemetry.analyzer.service.hub;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public interface HubEventHandler {
    Class<?> getPayloadType();

    void handle(HubEventAvro event);
}
