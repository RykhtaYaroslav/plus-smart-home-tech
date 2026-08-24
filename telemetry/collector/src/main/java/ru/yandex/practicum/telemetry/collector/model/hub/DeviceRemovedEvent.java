package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Событие удаления зарегистрированного устройства из хаба.
 *
 * <p>Содержит идентификатор устройства, которое больше не должно
 * считаться подключённым к данному хабу.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class DeviceRemovedEvent extends HubEvent {
    private String id;

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_REMOVED;
    }
}
