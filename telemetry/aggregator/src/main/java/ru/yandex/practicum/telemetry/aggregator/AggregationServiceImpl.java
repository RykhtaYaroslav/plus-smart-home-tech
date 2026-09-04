package ru.yandex.practicum.telemetry.aggregator;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Optional;

@Service
public class AggregationServiceImpl implements AggregationService {
    @Override
    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        return Optional.empty();
    }
}
