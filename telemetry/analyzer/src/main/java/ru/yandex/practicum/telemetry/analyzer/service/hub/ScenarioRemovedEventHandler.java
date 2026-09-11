package ru.yandex.practicum.telemetry.analyzer.service.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

@Component
@RequiredArgsConstructor
public class ScenarioRemovedEventHandler implements HubEventHandler {
    private final ScenarioRepository scenarioRepository;
    private final ScenarioRemovalService scenarioRemovalService;

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

        scenarioRemovalService.remove(scenario);
    }
}
