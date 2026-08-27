package ru.yandex.practicum.telemetry.collector.model.hub;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

/**
 * Базовый тип для событий, связанных с хабом умного дома.
 *
 * <p>Содержит общие для всех событий хаба данные:
 * идентификатор хаба и время возникновения события.
 * Конкретный тип события определяется реализацией метода {@link #getType()}.</p>
 *
 * <p>Используется как базовый тип при полиморфной десериализации
 * входящих JSON-сообщений.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeviceAddedEvent.class, name = "DEVICE_ADDED"),
        @JsonSubTypes.Type(value = DeviceRemovedEvent.class, name = "DEVICE_REMOVED"),
        @JsonSubTypes.Type(value = ScenarioAddedEvent.class, name = "SCENARIO_ADDED"),
        @JsonSubTypes.Type(value = ScenarioRemovedEvent.class, name = "SCENARIO_REMOVED")
})
public abstract class HubEvent {
    private String hubId;
    private Instant timestamp = Instant.now();

    public abstract HubEventType getType();
}
