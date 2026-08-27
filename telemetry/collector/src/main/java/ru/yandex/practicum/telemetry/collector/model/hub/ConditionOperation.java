package ru.yandex.practicum.telemetry.collector.model.hub;

/**
 * Перечисление операций сравнения, используемых при проверке
 * условий активации сценария.
 *
 * <p>Определяет способ сравнения текущего показания устройства
 * с заданным в сценарии значением.</p>
 */
public enum ConditionOperation {
    EQUALS,
    GREATER_THAN,
    LOWER_THAN
}
