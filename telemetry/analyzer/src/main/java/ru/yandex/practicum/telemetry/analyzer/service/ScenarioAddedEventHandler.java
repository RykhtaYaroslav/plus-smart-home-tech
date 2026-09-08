package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.ActionType;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.ConditionOperation;
import ru.yandex.practicum.telemetry.analyzer.model.ConditionType;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioActionId;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioConditionId;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ScenarioAddedEventHandler implements HubEventHandler {
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Override
    public Class<?> getPayloadType() {
        return ScenarioAddedEventAvro.class;
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) event.getPayload();

        String hubId = event.getHubId();
        String name = payload.getName();

        Scenario scenario = scenarioRepository.findByHubIdAndName(hubId, name).orElseGet(() -> {
            Scenario newScenario = new Scenario();
            newScenario.setHubId(hubId);
            newScenario.setName(name);
            return newScenario;
        });

        List<Sensor> sensors = getSensorsListOrThrow(payload, hubId);
        Map<String, Sensor> sensorsById = sensors.stream()
                .collect(Collectors.toMap(Sensor::getId, Function.identity()));

        if (scenario.getId() == null) {
            scenario = scenarioRepository.save(scenario);
        } else {
            removeScenarioDetails(scenario);
            scenario.getConditions().clear();
            scenario.getActions().clear();
        }

        Scenario savedScenario = scenario;
        payload.getConditions().stream()
                .map(condition ->
                        createScenarioCondition(savedScenario, sensorsById.get(condition.getSensorId()), condition))
                .forEach(savedScenario.getConditions()::add);

        payload.getActions().stream()
                .map(action ->
                        createScenarioAction(savedScenario, sensorsById.get(action.getSensorId()), action))
                .forEach(savedScenario.getActions()::add);

        scenarioRepository.flush();
    }

    private ScenarioCondition createScenarioCondition(Scenario scenario, Sensor sensor,
                                                     ScenarioConditionAvro conditionAvro) {
        Condition condition = new Condition();
        condition.setType(ConditionType.valueOf(conditionAvro.getType().name()));
        condition.setOperation(ConditionOperation.valueOf(conditionAvro.getOperation().name()));
        condition.setValue(toConditionValue(conditionAvro.getValue()));
        condition = conditionRepository.save(condition);

        ScenarioConditionId id = new ScenarioConditionId();
        id.setScenarioId(scenario.getId());
        id.setSensorId(sensor.getId());
        id.setConditionId(condition.getId());

        ScenarioCondition link = new ScenarioCondition();
        link.setId(id);
        link.setScenario(scenario);
        link.setSensor(sensor);
        link.setCondition(condition);
        return link;
    }

    private ScenarioAction createScenarioAction(Scenario scenario, Sensor sensor, DeviceActionAvro actionAvro) {
        Action action = new Action();
        action.setType(ActionType.valueOf(actionAvro.getType().name()));
        action.setValue(actionAvro.getValue());
        action = actionRepository.save(action);

        ScenarioActionId id = new ScenarioActionId();
        id.setScenarioId(scenario.getId());
        id.setSensorId(sensor.getId());
        id.setActionId(action.getId());

        ScenarioAction link = new ScenarioAction();
        link.setId(id);
        link.setScenario(scenario);
        link.setSensor(sensor);
        link.setAction(action);
        return link;
    }

    private void removeScenarioDetails(Scenario scenario) {
        List<Long> conditionIds = scenarioConditionRepository.findConditionIdsByScenarioId(scenario.getId());
        List<Long> actionIds = scenarioActionRepository.findActionIdsByScenarioId(scenario.getId());

        // Execute both link deletions before deleting the referenced entities (FK constraints).
        scenarioConditionRepository.deleteAllByScenarioId(scenario.getId());
        scenarioActionRepository.deleteAllByScenarioId(scenario.getId());

        // Delete only this scenario's old entities that are no longer referenced elsewhere.
        if (!conditionIds.isEmpty()) {
            conditionRepository.deleteUnreferencedByIdIn(conditionIds);
        }
        if (!actionIds.isEmpty()) {
            actionRepository.deleteUnreferencedByIdIn(actionIds);
        }
    }

    private Integer toConditionValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? 1 : 0;
        }
        throw new IllegalArgumentException("Неподдерживаемый тип значения условия: " + value.getClass().getName());
    }

    private List<Sensor> getSensorsListOrThrow(ScenarioAddedEventAvro payload, String hubId) {
        Set<String> ids = Stream.concat(
                        payload.getConditions().stream().map(ScenarioConditionAvro::getSensorId),
                        payload.getActions().stream().map(DeviceActionAvro::getSensorId)
                )
                .collect(Collectors.toSet());

        List<Sensor> sensors = sensorRepository.findAllByIdInAndHubId(new ArrayList<>(ids), hubId);

        if (sensors.size() != ids.size()) {
            Set<String> foundIds = sensors.stream()
                    .map(Sensor::getId)
                    .collect(Collectors.toSet());

            Set<String> missingIds = new HashSet<>(ids);
            missingIds.removeAll(foundIds);

            throw new IllegalArgumentException(
                    "Для хаба " + hubId +
                            " не найдены устройства: " + missingIds
            );
        }

        return sensors;
    }
}
