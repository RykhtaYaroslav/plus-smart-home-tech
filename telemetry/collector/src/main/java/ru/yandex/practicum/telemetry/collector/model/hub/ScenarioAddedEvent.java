package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Событие добавления сценария умного дома.
 *
 * <p>Содержит название сценария, набор условий его активации
 * и список действий, которые необходимо выполнить
 * после выполнения этих условий.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
public class ScenarioAddedEvent extends HubEvent {
    private String name;
    private List<ScenarioCondition> conditions;
    private List<DeviceAction> actions;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_ADDED;
    }
}
