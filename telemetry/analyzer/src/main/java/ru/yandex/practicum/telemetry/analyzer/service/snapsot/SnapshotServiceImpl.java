package ru.yandex.practicum.telemetry.analyzer.service.snapsot;

import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;
import ru.yandex.practicum.telemetry.analyzer.model.ConditionType;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SnapshotServiceImpl implements SnapshotService {
    private final HubRouterControllerBlockingStub hubRouterClient;
    private final ScenarioRepository scenarioRepository;

    public SnapshotServiceImpl(
            ScenarioRepository scenarioRepository,
            @GrpcClient("hub-router")
            HubRouterControllerBlockingStub hubRouterClient
    ) {
        this.scenarioRepository = scenarioRepository;
        this.hubRouterClient = hubRouterClient;
    }

    @Override
    @Transactional(readOnly = true)
    public void handle(SensorsSnapshotAvro snapshot) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(snapshot.getHubId());

        Map<String, SensorStateAvro> states = snapshot.getSensorsState();

        for (Scenario scenario : scenarios) {
            Set<ScenarioCondition> conditions = scenario.getConditions();

            boolean matches = conditions.stream()
                    .allMatch(sc -> {
                        String sensorId = sc.getSensor().getId();
                        Condition condition = sc.getCondition();

                        SensorStateAvro state = states.get(sensorId);

                        if (state == null) {
                            return false;
                        }

                        return checkCondition(condition, state);
                    });

            if (matches) {
                executeActions(snapshot, scenario);
            }
        }
    }

    private void executeActions(SensorsSnapshotAvro snapshot, Scenario scenario) {
        for (ScenarioAction scenarioAction : scenario.getActions()) {

            Action action = scenarioAction.getAction();
            Sensor sensor = scenarioAction.getSensor();

            DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                    .setSensorId(sensor.getId())
                    .setType(ActionTypeProto.valueOf(action.getType().name()));

            if (action.getValue() != null) {
                actionBuilder.setValue(action.getValue());
            }

            Instant instant = snapshot.getTimestamp();

            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(snapshot.getHubId())
                    .setScenarioName(scenario.getName())
                    .setAction(actionBuilder.build())
                    .setTimestamp(timestamp)
                    .build();

            hubRouterClient.handleDeviceAction(request);
        }
    }

    private boolean checkCondition(Condition condition, SensorStateAvro state) {
        Integer sensorValue = getSensorValue(condition.getType(), state.getData());

        if (sensorValue == null) {
            return false;
        }

        Integer conditionValue = condition.getValue();
        if (conditionValue == null) {
            return false;
        }

        return switch (condition.getOperation()) {
            case EQUALS -> sensorValue.equals(conditionValue);
            case GREATER_THAN -> sensorValue > conditionValue;
            case LOWER_THAN -> sensorValue < conditionValue;
        };
    }

    private Integer getSensorValue(ConditionType conditionType, Object data) {
        return switch (conditionType) {
            case MOTION -> {
                if (data instanceof MotionSensorAvro motion) {
                    yield motion.getMotion() ? 1 : 0;
                }
                yield null;
            }

            case LUMINOSITY -> {
                if (data instanceof LightSensorAvro light) {
                    yield light.getLuminosity();
                }
                yield null;
            }

            case SWITCH -> {
                if (data instanceof SwitchSensorAvro switchSensor) {
                    yield switchSensor.getState() ? 1 : 0;
                }
                yield null;
            }

            case TEMPERATURE -> {
                if (data instanceof TemperatureSensorAvro temperature) {
                    yield temperature.getTemperatureC();
                }

                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getTemperatureC();
                }

                yield null;
            }

            case CO2LEVEL -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getCo2Level();
                }
                yield null;
            }

            case HUMIDITY -> {
                if (data instanceof ClimateSensorAvro climate) {
                    yield climate.getHumidity();
                }
                yield null;
            }
        };
    }
}
