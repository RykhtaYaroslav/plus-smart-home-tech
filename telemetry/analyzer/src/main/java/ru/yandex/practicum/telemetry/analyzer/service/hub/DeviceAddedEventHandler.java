package ru.yandex.practicum.telemetry.analyzer.service.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.SensorRepository;

@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {
    private final SensorRepository repository;

    @Override
    public Class<?> getPayloadType() {
        return DeviceAddedEventAvro.class;
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        DeviceAddedEventAvro payload = (DeviceAddedEventAvro) event.getPayload();

        if (repository.existsById(payload.getId())) {
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setHubId(event.getHubId());
        sensor.setId(payload.getId());

        repository.save(sensor);
    }
}
