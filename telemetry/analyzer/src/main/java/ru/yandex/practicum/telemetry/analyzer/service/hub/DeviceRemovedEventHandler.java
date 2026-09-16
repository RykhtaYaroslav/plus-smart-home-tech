package ru.yandex.practicum.telemetry.analyzer.service.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;

@Component
@RequiredArgsConstructor
public class DeviceRemovedEventHandler implements HubEventHandler {
    private final SensorRepository repository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioRemovalService scenarioRemovalService;

    @Override
    public Class<?> getPayloadType() {
        return DeviceRemovedEventAvro.class;
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) event.getPayload();
        Sensor sensor = repository.findByIdAndHubId(payload.getId(), event.getHubId()).orElse(null);
        if (sensor == null) {
            return;
        }
        scenarioRepository.findAllUsingSensor(event.getHubId(), sensor.getId())
                .forEach(scenarioRemovalService::remove);
        repository.delete(sensor);
        repository.flush();
    }
}
