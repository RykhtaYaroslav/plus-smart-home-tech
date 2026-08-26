package ru.yandex.practicum.telemetry.collector.model.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent;

import java.util.Objects;

@Component
public class SensorEventMapperImpl implements SensorEventMapper {

    @Override
    public SensorEventAvro map(SensorEvent event) {
        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(SensorEvent event) {
        Objects.requireNonNull(event, "Sensor event must not be null");

        return switch (event) {
            case LightSensorEvent light -> map(light);
            case MotionSensorEvent motion -> map(motion);
            case SwitchSensorEvent switchEvent -> map(switchEvent);
            case ClimateSensorEvent climate -> map(climate);
            case TemperatureSensorEvent temperature -> map(temperature);
            default -> throw new IllegalStateException("Unsupported sensor event type: " + event.getClass().getName());
        };
    }

    private LightSensorAvro map (LightSensorEvent light) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(light.getLinkQuality())
                .setLuminosity(light.getLuminosity())
                .build();
    }

    private MotionSensorAvro map(MotionSensorEvent motion) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(motion.getLinkQuality())
                .setMotion(motion.isMotion())
                .setVoltage(motion.getVoltage())
                .build();
    }

    private SwitchSensorAvro map(SwitchSensorEvent switchEvent) {
        return SwitchSensorAvro.newBuilder()
                .setState(switchEvent.isState())
                .build();
    }

    private ClimateSensorAvro map(ClimateSensorEvent climate) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(climate.getTemperatureC())
                .setHumidity(climate.getHumidity())
                .setCo2Level(climate.getCo2Level())
                .build();
    }

    private TemperatureSensorAvro map(TemperatureSensorEvent temperature) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(temperature.getTemperatureC())
                .setTemperatureF(temperature.getTemperatureF())
                .build();
    }
}
