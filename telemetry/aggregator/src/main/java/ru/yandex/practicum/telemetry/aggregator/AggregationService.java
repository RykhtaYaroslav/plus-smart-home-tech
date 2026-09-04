package ru.yandex.practicum.telemetry.aggregator;

import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Optional;

public interface AggregationService {
    Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event);
}
