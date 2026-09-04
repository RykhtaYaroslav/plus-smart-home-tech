package ru.yandex.practicum.telemetry.collector.model.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioRemovedEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;
import ru.yandex.practicum.telemetry.collector.model.hub.ScenarioRemovedEvent;

import java.time.Instant;

@Component
public class ScenarioRemovedEventMapper implements HubEventMapper {
    @Override
    public HubEventType getMessageType() {
        return HubEventType.SCENARIO_REMOVED;
    }

    @Override
    public HubEventAvro mapToAvro(HubEvent event) {
        ScenarioRemovedEvent removed = (ScenarioRemovedEvent) event;

        return HubEventAvro.newBuilder()
                .setHubId(removed.getHubId())
                .setTimestamp(removed.getTimestamp())
                .setPayload(ScenarioRemovedEventAvro.newBuilder()
                        .setName(removed.getName())
                        .build())
                .build();
    }

    @Override
    public ScenarioRemovedEvent mapToModel(HubEventProto event) {
        ScenarioRemovedEventProto payload = event.getScenarioRemoved();
        ScenarioRemovedEvent removed = new ScenarioRemovedEvent();
        removed.setHubId(event.getHubId());
        removed.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        removed.setName(payload.getName());
        return removed;
    }
}
