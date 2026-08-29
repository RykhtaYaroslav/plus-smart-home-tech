package ru.yandex.practicum.telemetry.collector.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceRemovedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.DeviceRemovedEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;

import java.time.Instant;

@Component
public class DeviceRemovedEventMapper implements HubEventMapper {
    @Override
    public HubEventType getMessageType() {
        return HubEventType.DEVICE_REMOVED;
    }

    @Override
    public HubEventAvro mapToAvro(HubEvent event) {
        DeviceRemovedEvent removed = (DeviceRemovedEvent) event;

        return HubEventAvro.newBuilder()
                .setHubId(removed.getHubId())
                .setTimestamp(removed.getTimestamp())
                .setPayload(DeviceRemovedEventAvro.newBuilder()
                        .setId(removed.getId())
                        .build())
                .build();
    }

    @Override
    public DeviceRemovedEvent mapToModel(HubEventProto event) {
        DeviceRemovedEventProto payload = event.getDeviceRemoved();
        DeviceRemovedEvent removed = new DeviceRemovedEvent();
        removed.setHubId(event.getHubId());
        removed.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        removed.setId(payload.getId());
        return removed;
    }
}
