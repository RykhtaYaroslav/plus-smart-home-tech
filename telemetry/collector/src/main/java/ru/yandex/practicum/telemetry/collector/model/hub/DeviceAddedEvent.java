package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Событие регистрации нового устройства в хабе.
 *
 * <p>Содержит идентификатор добавляемого устройства и его тип.
 * Используется при получении от Hub Router информации
 * о подключении нового устройства к системе умного дома.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class DeviceAddedEvent extends HubEvent {
    private String id;
    private DeviceType deviceType;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_ADDED;
    }
}
