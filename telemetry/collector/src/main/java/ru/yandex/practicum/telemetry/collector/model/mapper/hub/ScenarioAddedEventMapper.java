package ru.yandex.practicum.telemetry.collector.model.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.telemetry.collector.model.hub.ActionType;
import ru.yandex.practicum.telemetry.collector.model.hub.ConditionOperation;
import ru.yandex.practicum.telemetry.collector.model.hub.ConditionType;
import ru.yandex.practicum.telemetry.collector.model.hub.DeviceAction;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEventType;
import ru.yandex.practicum.telemetry.collector.model.hub.ScenarioAddedEvent;
import ru.yandex.practicum.telemetry.collector.model.hub.ScenarioCondition;

import java.time.Instant;

@Component
public class ScenarioAddedEventMapper implements HubEventMapper {
    @Override
    public HubEventType getMessageType() {
        return HubEventType.SCENARIO_ADDED;
    }

    @Override
    public HubEventAvro mapToAvro(HubEvent event) {
        ScenarioAddedEvent added = (ScenarioAddedEvent) event;

        return HubEventAvro.newBuilder()
                .setHubId(added.getHubId())
                .setTimestamp(added.getTimestamp())
                .setPayload(ScenarioAddedEventAvro.newBuilder()
                        .setName(added.getName())
                        .setConditions(added.getConditions().stream()
                                .map(this::mapToAvro)
                                .toList())
                        .setActions(added.getActions().stream()
                                .map(this::mapToAvro)
                                .toList())
                        .build())
                .build();
    }

    @Override
    public ScenarioAddedEvent mapToModel(HubEventProto event) {
        ScenarioAddedEventProto payload = event.getScenarioAdded();
        ScenarioAddedEvent added = new ScenarioAddedEvent();
        added.setHubId(event.getHubId());
        added.setTimestamp(Instant.ofEpochSecond(
                event.getTimestamp().getSeconds(),
                event.getTimestamp().getNanos()
        ));
        added.setName(payload.getName());
        added.setConditions(payload.getConditionList().stream()
                .map(this::mapToModel)
                .toList());
        added.setActions(payload.getActionList().stream()
                .map(this::mapToModel)
                .toList());
        return added;
    }

    private ScenarioConditionAvro mapToAvro(ScenarioCondition condition) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()))
                .setValue(condition.getValue())
                .build();
    }

    private DeviceActionAvro mapToAvro(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType().name()))
                .setValue(action.getValue())
                .build();
    }

    private ScenarioCondition mapToModel(ScenarioConditionProto conditionProto) {
        ScenarioCondition condition = new ScenarioCondition();
        condition.setSensorId(conditionProto.getSensorId());
        condition.setType(ConditionType.valueOf(conditionProto.getType().name()));
        condition.setOperation(ConditionOperation.valueOf(conditionProto.getOperation().name()));
        condition.setValue(switch (conditionProto.getValueCase()) {
            case BOOL_VALUE -> conditionProto.getBoolValue();
            case INT_VALUE -> conditionProto.getIntValue();
            case VALUE_NOT_SET -> null;
        });
        return condition;
    }

    private DeviceAction mapToModel(DeviceActionProto actionProto) {
        DeviceAction action = new DeviceAction();
        action.setSensorId(actionProto.getSensorId());
        action.setType(ActionType.valueOf(actionProto.getType().name()));
        action.setValue(actionProto.hasValue() ? actionProto.getValue() : null);
        return action;
    }
}
