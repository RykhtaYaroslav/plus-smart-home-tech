package ru.yandex.practicum.telemetry.analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc.HubRouterControllerBlockingStub;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.snapsot.SnapshotServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SnapshotServiceImplTest {
    private final ScenarioRepository repository = mock(ScenarioRepository.class);
    private final HubRouterControllerBlockingStub client = mock(HubRouterControllerBlockingStub.class);
    private final SnapshotServiceImpl service = new SnapshotServiceImpl(repository, client, 5000);
    private final Instant timestamp = Instant.parse("2026-09-11T10:15:30.123Z");
    private Scenario scenario;

    @BeforeEach
    void setUp() {
        lenient().when(client.withDeadlineAfter(5000, TimeUnit.MILLISECONDS)).thenReturn(client);
        scenario = new Scenario();
        scenario.setHubId("hub-1");
        scenario.setName("climate");
        addAction(ActionType.SET_VALUE, 25);
        when(repository.findByHubId("hub-1")).thenReturn(List.of(scenario));
    }

    @Test
    void sendsAllActionsWithScenarioAndSnapshotTimestamp() {
        addCondition("sensor-1", ConditionType.TEMPERATURE, ConditionOperation.GREATER_THAN, 20);
        addAction(ActionType.ACTIVATE, null);

        service.handle(snapshot(new TemperatureSensorAvro(21, 70)));

        ArgumentCaptor<DeviceActionRequest> captor = ArgumentCaptor.forClass(DeviceActionRequest.class);
        verify(client, times(2)).handleDeviceAction(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(request -> {
            assertThat(request.getHubId()).isEqualTo("hub-1");
            assertThat(request.getScenarioName()).isEqualTo("climate");
            assertThat(request.getAction().getSensorId()).isEqualTo("actuator-1");
            assertThat(request.getTimestamp().getSeconds()).isEqualTo(timestamp.getEpochSecond());
            assertThat(request.getTimestamp().getNanos()).isEqualTo(timestamp.getNano());
        });
        assertThat(captor.getAllValues()).anySatisfy(request -> {
            assertThat(request.getAction().getType()).isEqualTo(ActionTypeProto.SET_VALUE);
            assertThat(request.getAction().hasValue()).isTrue();
            assertThat(request.getAction().getValue()).isEqualTo(25);
        }).anySatisfy(request -> {
            assertThat(request.getAction().getType()).isEqualTo(ActionTypeProto.ACTIVATE);
            assertThat(request.getAction().hasValue()).isFalse();
        });
    }

    @ParameterizedTest
    @MethodSource("conditionCases")
    void evaluatesSensorConditions(ConditionType type, Object data, ConditionOperation operation,
                                  int value, boolean matches) {
        addCondition("sensor-1", type, operation, value);

        service.handle(snapshot(data));

        if (matches) {
            verify(client).handleDeviceAction(any(DeviceActionRequest.class));
        } else {
            verifyNoInteractions(client);
        }
    }

    static Stream<Arguments> conditionCases() {
        return Stream.of(
                Arguments.of(ConditionType.MOTION, new MotionSensorAvro(100, true, 3), ConditionOperation.EQUALS, 1, true),
                Arguments.of(ConditionType.MOTION, new MotionSensorAvro(100, false, 3), ConditionOperation.EQUALS, 0, true),
                Arguments.of(ConditionType.SWITCH, new SwitchSensorAvro(true), ConditionOperation.EQUALS, 1, true),
                Arguments.of(ConditionType.SWITCH, new SwitchSensorAvro(false), ConditionOperation.EQUALS, 0, true),
                Arguments.of(ConditionType.LUMINOSITY, new LightSensorAvro(100, 50), ConditionOperation.LOWER_THAN, 60, true),
                Arguments.of(ConditionType.TEMPERATURE, new TemperatureSensorAvro(20, 68), ConditionOperation.EQUALS, 20, true),
                Arguments.of(ConditionType.TEMPERATURE, new ClimateSensorAvro(20, 50, 800), ConditionOperation.EQUALS, 20, true),
                Arguments.of(ConditionType.HUMIDITY, new ClimateSensorAvro(20, 50, 800), ConditionOperation.EQUALS, 50, true),
                Arguments.of(ConditionType.CO2LEVEL, new ClimateSensorAvro(20, 50, 800), ConditionOperation.GREATER_THAN, 700, true),
                Arguments.of(ConditionType.TEMPERATURE, new TemperatureSensorAvro(20, 68), ConditionOperation.GREATER_THAN, 20, false),
                Arguments.of(ConditionType.TEMPERATURE, new TemperatureSensorAvro(20, 68), ConditionOperation.LOWER_THAN, 20, false),
                Arguments.of(ConditionType.TEMPERATURE, new TemperatureSensorAvro(20, 68), ConditionOperation.EQUALS, 21, false),
                Arguments.of(ConditionType.TEMPERATURE, new SwitchSensorAvro(true), ConditionOperation.EQUALS, 1, false)
        );
    }

    @Test
    void skipsScenarioWhenOneOfItsSensorsIsMissing() {
        addCondition("sensor-1", ConditionType.TEMPERATURE, ConditionOperation.EQUALS, 20);
        addCondition("missing", ConditionType.TEMPERATURE, ConditionOperation.EQUALS, 20);

        service.handle(snapshot(new TemperatureSensorAvro(20, 68)));

        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @EnumSource(ConditionOperation.class)
    void skipsConditionWithoutValue(ConditionOperation operation) {
        addCondition("sensor-1", ConditionType.TEMPERATURE, operation, null);

        service.handle(snapshot(new TemperatureSensorAvro(20, 68)));

        verifyNoInteractions(client);
    }

    private SensorsSnapshotAvro snapshot(Object data) {
        return new SensorsSnapshotAvro("hub-1", timestamp,
                Map.of("sensor-1", new SensorStateAvro(timestamp, data)));
    }

    private void addCondition(String sensorId, ConditionType type, ConditionOperation operation, Integer value) {
        Sensor sensor = new Sensor();
        sensor.setId(sensorId);
        Condition condition = new Condition();
        condition.setType(type);
        condition.setOperation(operation);
        condition.setValue(value);
        ScenarioCondition link = new ScenarioCondition();
        link.setSensor(sensor);
        link.setCondition(condition);
        scenario.getConditions().add(link);
    }

    private void addAction(ActionType type, Integer value) {
        Sensor sensor = new Sensor();
        sensor.setId("actuator-1");
        Action action = new Action();
        action.setType(type);
        action.setValue(value);
        ScenarioAction link = new ScenarioAction();
        link.setSensor(sensor);
        link.setAction(action);
        scenario.getActions().add(link);
    }
}
