package ru.yandex.practicum.telemetry.analyzer.service.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScenarioRemovedEventHandler implements HubEventHandler {
    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Override
    public Class<?> getPayloadType() {
        return ScenarioRemovedEventAvro.class;
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        ScenarioRemovedEventAvro payload = (ScenarioRemovedEventAvro) event.getPayload();
        Scenario scenario = scenarioRepository.findByHubIdAndName(event.getHubId(), payload.getName())
                .orElse(null);
        if (scenario == null) {
            return;
        }

        List<Long> conditionIds = scenarioConditionRepository.findConditionIdsByScenarioId(scenario.getId());
        List<Long> actionIds = scenarioActionRepository.findActionIdsByScenarioId(scenario.getId());

        scenarioConditionRepository.deleteAllByScenarioId(scenario.getId());
        scenarioActionRepository.deleteAllByScenarioId(scenario.getId());

        if (!conditionIds.isEmpty()) {
            conditionRepository.deleteUnreferencedByIdIn(conditionIds);
        }
        if (!actionIds.isEmpty()) {
            actionRepository.deleteUnreferencedByIdIn(actionIds);
        }

        scenario.getConditions().clear();
        scenario.getActions().clear();

        scenarioRepository.deleteAllByIdInBatch(List.of(scenario.getId()));
    }
}
