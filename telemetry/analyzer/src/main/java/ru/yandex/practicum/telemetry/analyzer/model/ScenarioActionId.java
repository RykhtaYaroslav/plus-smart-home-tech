package ru.yandex.practicum.telemetry.analyzer.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ScenarioActionId implements Serializable {
    @Column(name = "scenario_id")
    private Long scenarioId;
    @Column(name = "sensor_id")
    private String sensorId;
    @Column(name = "action_id")
    private Long actionId;
}
