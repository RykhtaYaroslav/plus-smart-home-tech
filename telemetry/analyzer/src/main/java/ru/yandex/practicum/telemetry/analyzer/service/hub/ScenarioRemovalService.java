package ru.yandex.practicum.telemetry.analyzer.service.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioRemovalService {
    private final ScenarioRepository scenarioRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void remove(Scenario scenario) {
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
