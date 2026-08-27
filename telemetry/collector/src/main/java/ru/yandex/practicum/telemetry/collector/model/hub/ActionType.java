package ru.yandex.practicum.telemetry.collector.model.hub;

/**
 * Перечисление действий, которые могут быть выполнены устройством
 * при активации сценария умного дома.
 *
 * <p>Определяет требуемую операцию над устройством:
 * включение, выключение, инвертирование текущего состояния
 * или установку конкретного значения.</p>
 */
public enum ActionType {
    ACTIVATE,
    DEACTIVATE,
    INVERSE,
    SET_VALUE
}
