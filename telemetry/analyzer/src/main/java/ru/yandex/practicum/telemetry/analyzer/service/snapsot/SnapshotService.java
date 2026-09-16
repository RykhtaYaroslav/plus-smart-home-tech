package ru.yandex.practicum.telemetry.analyzer.service.snapsot;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public interface SnapshotService {
    void handle(SensorsSnapshotAvro snapshotAvro);
}
