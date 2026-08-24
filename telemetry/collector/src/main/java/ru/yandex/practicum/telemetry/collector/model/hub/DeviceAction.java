package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Описание одного действия над устройством,
 * выполняемого при активации сценария умного дома.
 *
 * <p>Содержит идентификатор устройства, тип выполняемого действия
 * и необязательное числовое значение, которое используется
 * для действий, требующих установки конкретного параметра.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class DeviceAction {
    private String sensorId;
    private ActionType type;
    private Integer value;
}
