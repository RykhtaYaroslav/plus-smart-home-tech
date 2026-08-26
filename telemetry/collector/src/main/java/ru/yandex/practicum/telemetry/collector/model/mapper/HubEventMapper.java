package ru.yandex.practicum.telemetry.collector.model.mapper;

import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;

public interface HubEventMapper {

    HubEventAvro map(HubEvent event);
}