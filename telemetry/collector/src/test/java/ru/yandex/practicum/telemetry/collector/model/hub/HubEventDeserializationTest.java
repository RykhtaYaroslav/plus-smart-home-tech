package ru.yandex.practicum.telemetry.collector.model.hub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HubEventDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeDeviceAddedEvent() throws Exception {
        String json = """
                {
                  "hubId": "hub.12345",
                  "timestamp": "2024-08-06T15:11:24.157Z",
                  "type": "DEVICE_ADDED",
                  "id": "sensor.light.3",
                  "deviceType": "MOTION_SENSOR"
                }
                """;

        HubEvent event = objectMapper.readValue(json, HubEvent.class);

        assertThat(event).isInstanceOf(DeviceAddedEvent.class);

        DeviceAddedEvent deviceAddedEvent = (DeviceAddedEvent) event;

        assertThat(deviceAddedEvent.getHubId()).isEqualTo("hub.12345");
        assertThat(deviceAddedEvent.getTimestamp())
                .isEqualTo(Instant.parse("2024-08-06T15:11:24.157Z"));
        assertThat(deviceAddedEvent.getType()).isEqualTo(HubEventType.DEVICE_ADDED);
        assertThat(deviceAddedEvent.getId()).isEqualTo("sensor.light.3");
        assertThat(deviceAddedEvent.getDeviceType()).isEqualTo(DeviceType.MOTION_SENSOR);
    }
}
