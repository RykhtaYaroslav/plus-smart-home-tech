package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Событие удаления сценария умного дома.
 *
 * <p>Содержит название сценария, который должен быть удалён
 * из конфигурации соответствующего хаба.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ScenarioRemovedEvent extends HubEvent {
    private String name;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_REMOVED;
    }
}
