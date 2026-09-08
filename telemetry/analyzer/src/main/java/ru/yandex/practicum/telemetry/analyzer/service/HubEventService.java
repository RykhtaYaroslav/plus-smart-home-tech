package ru.yandex.practicum.telemetry.analyzer.service;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public interface HubEventService {
    void handle(HubEventAvro hubEventAvro);
}
