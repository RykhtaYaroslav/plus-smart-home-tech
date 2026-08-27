package ru.yandex.practicum.telemetry.collector.model.hub;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Описание одного условия активации сценария умного дома.
 *
 * <p>Условие связывает конкретное устройство с типом его показаний,
 * операцией сравнения и значением, с которым необходимо выполнить
 * сравнение.</p>
 *
 * <p>Поле {@code value} может содержать целое число,
 * логическое значение или {@code null} в зависимости от типа условия.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ScenarioCondition {
    private String sensorId;
    private ConditionType type;
    private ConditionOperation operation;
    private Object value;
}
