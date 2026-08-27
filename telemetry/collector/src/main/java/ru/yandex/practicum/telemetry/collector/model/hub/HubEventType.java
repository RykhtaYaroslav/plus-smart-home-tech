package ru.yandex.practicum.telemetry.collector.model.hub;

/**
 * Перечисление типов событий, которые могут происходить в хабе.
 *
 * <p>Используется для определения конкретного типа входящего события
 * при десериализации JSON и для различения операций с устройствами
 * и сценариями умного дома.</p>
 */
public enum HubEventType {
    DEVICE_ADDED,
    DEVICE_REMOVED,
    SCENARIO_ADDED,
    SCENARIO_REMOVED
}
