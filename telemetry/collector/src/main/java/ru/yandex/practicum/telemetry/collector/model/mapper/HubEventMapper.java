package ru.yandex.practicum.telemetry.collector.model.mapper;

import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;

public interface HubEventMapper {
    HubEventType getMessageType();

    HubEventAvro mapToAvro(HubEvent event);

    HubEvent mapToModel(HubEventProto event);
}