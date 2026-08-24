package ru.yandex.practicum.telemetry.collector.model.hub;

/**
 * Перечисление поддерживаемых типов устройств умного дома.
 *
 * <p>Используется при регистрации нового устройства в хабе
 * и определяет категорию подключаемого датчика или переключателя.</p>
 */
public enum DeviceType {
    MOTION_SENSOR,
    TEMPERATURE_SENSOR,
    LIGHT_SENSOR,
    CLIMATE_SENSOR,
    SWITCH_SENSOR
}
