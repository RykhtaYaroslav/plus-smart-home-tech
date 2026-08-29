package ru.yandex.practicum.telemetry.collector.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.DeviceAddedEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.DeviceType;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;

import java.time.Instant;

@Component
public class DeviceAddedEventMapper implements HubEventMapper {
    @Override
    public HubEventType getMessageType() {
        return HubEventType.DEVICE_ADDED;
    }

    @Override
    public HubEventAvro mapToAvro(HubEvent event) {
        DeviceAddedEvent added = (DeviceAddedEvent) event;

        return HubEventAvro.newBuilder()
                .setHubId(added.getHubId())
                .setTimestamp(added.getTimestamp())
                .setPayload(DeviceAddedEventAvro.newBuilder()
                        .setId(added.getId())
                        .setType(DeviceTypeAvro.valueOf(added.getDeviceType().name()))
                        .build())
                .build();
    }

    @Override
    public DeviceAddedEvent mapToModel(HubEventProto event) {
        DeviceAddedEventProto payload = event.getDeviceAdded();
        DeviceAddedEvent added = new DeviceAddedEvent();
        added.setHubId(event.getHubId());
        added.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        added.setId(payload.getId());
        added.setDeviceType(DeviceType.valueOf(payload.getType().name()));
        return added;
    }
}
