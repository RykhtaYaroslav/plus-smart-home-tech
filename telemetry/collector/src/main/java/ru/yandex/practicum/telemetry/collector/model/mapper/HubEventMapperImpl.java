package ru.yandex.practicum.telemetry.collector.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.hub.*;

@Component
public class HubEventMapperImpl implements HubEventMapper {

    @Override
    public HubEventAvro map(HubEvent event) {
        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(HubEvent event) {
        return switch (event) {
            case DeviceAddedEvent added -> map(added);
            case DeviceRemovedEvent removed -> map(removed);
            case ScenarioAddedEvent added -> map(added);
            case ScenarioRemovedEvent removed -> map(removed);
            default -> throw new IllegalArgumentException(
                    "Unsupported hub event type: " + event.getClass().getName()
            );
        };
    }

    private DeviceAddedEventAvro map(DeviceAddedEvent event) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(event.getId())
                .setType(DeviceTypeAvro.valueOf(event.getDeviceType().name()))
                .build();
    }

    private DeviceRemovedEventAvro map(DeviceRemovedEvent event) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(event.getId())
                .build();
    }

    private ScenarioAddedEventAvro map(ScenarioAddedEvent event) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(event.getName())
                .setConditions(
                        event.getConditions().stream()
                                .map(this::map)
                                .toList()
                )
                .setActions(
                        event.getActions().stream()
                                .map(this::map)
                                .toList()
                )
                .build();
    }

    private ScenarioRemovedEventAvro map(ScenarioRemovedEvent event) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(event.getName())
                .build();
    }

    private ScenarioConditionAvro map(ScenarioCondition condition) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()))
                .setValue(condition.getValue())
                .build();
    }

    private DeviceActionAvro map(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType().name()))
                .setValue(action.getValue())
                .build();
    }
}