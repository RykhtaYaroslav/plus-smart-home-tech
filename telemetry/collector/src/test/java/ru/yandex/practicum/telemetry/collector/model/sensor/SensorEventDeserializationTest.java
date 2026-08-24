package ru.yandex.practicum.telemetry.collector.model.sensor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SensorEventDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeLightSensorEvent() throws Exception {
        String json = """
                {
                  "id": "sensor.light.3",
                  "hubId": "hub-2",
                  "timestamp": "2024-08-06T16:54:03.129Z",
                  "type": "LIGHT_SENSOR_EVENT",
                  "linkQuality": 75,
                  "luminosity": 59
                }
                """;

        SensorEvent event = objectMapper.readValue(json, SensorEvent.class);

        assertThat(event).isInstanceOf(LightSensorEvent.class);

        LightSensorEvent lightEvent = (LightSensorEvent) event;

        assertThat(lightEvent.getId()).isEqualTo("sensor.light.3");
        assertThat(lightEvent.getHubId()).isEqualTo("hub-2");
        assertThat(lightEvent.getTimestamp())
                .isEqualTo(Instant.parse("2024-08-06T16:54:03.129Z"));

        assertThat(lightEvent.getType())
                .isEqualTo(SensorEventType.LIGHT_SENSOR_EVENT);

        assertThat(lightEvent.getLinkQuality()).isEqualTo(75);
        assertThat(lightEvent.getLuminosity()).isEqualTo(59);
    }
}