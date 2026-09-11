package ru.yandex.practicum.telemetry.analyzer.service;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Action;
import ru.yandex.practicum.telemetry.analyzer.model.ActionType;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.model.Sensor;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.snapsot.SnapshotServiceImpl;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Timeout(10)
class SnapshotGrpcFailureTest {
    private final ScenarioRepository repository = mock(ScenarioRepository.class);
    private final List<String> received = new CopyOnWriteArrayList<>();
    private ManagedChannel channel;
    private Server server;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        }
    }

    @Test
    void unavailableServerFailsWithoutReportingSuccess() {
        channel = InProcessChannelBuilder.forName(InProcessServerBuilder.generateName()).directExecutor().build();
        configureScenario();
        assertFailure(Status.Code.UNAVAILABLE, 2000);
    }

    @Test
    void stopsOnSecondActionFailureWithoutRepeatingFirstOrSendingThird() throws Exception {
        startServer((request, response) -> {
            if (request.getAction().getSensorId().equals("second")) {
                response.onError(Status.INTERNAL.withDescription("Action rejected").asRuntimeException());
            } else {
                response.onNext(Empty.getDefaultInstance());
                response.onCompleted();
            }
        });

        assertFailure(Status.Code.INTERNAL, 2000);

        assertThat(received).containsExactly("first", "second");
    }

    @Test
    void timesOutWhenServerDoesNotRespond() throws Exception {
        startServer((request, response) -> { });

        assertFailure(Status.Code.DEADLINE_EXCEEDED, 300);

        assertThat(received).containsExactly("first");
    }

    @Test
    void doesNotRepeatInverseWhenServerAppliedItButResponseWasLost() throws Exception {
        List<String> applied = new CopyOnWriteArrayList<>();
        startServer((request, response) -> {
            applied.add(request.getAction().getSensorId());
            response.onError(Status.UNAVAILABLE.withDescription("Response lost after execution").asRuntimeException());
        });

        assertFailure(Status.Code.UNAVAILABLE, 2000);

        assertThat(applied).containsExactly("first");
        assertThat(received).containsExactly("first");
    }

    private void assertFailure(Status.Code code, long timeoutMs) {
        var service = new SnapshotServiceImpl(repository, HubRouterControllerGrpc.newBlockingStub(channel), timeoutMs);
        var snapshot = new SensorsSnapshotAvro("hub-1", Instant.EPOCH, Map.of());
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> service.handle(snapshot));
        assertThat(exception.getStatus().getCode()).isEqualTo(code);
    }

    private void startServer(BiConsumer<DeviceActionRequest, StreamObserver<Empty>> behavior) throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new HubRouterControllerGrpc.HubRouterControllerImplBase() {
                    @Override
                    public void handleDeviceAction(DeviceActionRequest request, StreamObserver<Empty> response) {
                        received.add(request.getAction().getSensorId());
                        behavior.accept(request, response);
                    }
                }).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        configureScenario();
    }

    private void configureScenario() {
        Scenario scenario = new Scenario();
        scenario.setHubId("hub-1");
        scenario.setName("test");
        scenario.setActions(new LinkedHashSet<>());
        for (String id : List.of("first", "second", "third")) {
            Sensor sensor = new Sensor();
            sensor.setId(id);
            Action action = new Action();
            action.setType(ActionType.INVERSE);
            ScenarioAction link = new ScenarioAction();
            link.setSensor(sensor);
            link.setAction(action);
            scenario.getActions().add(link);
        }
        when(repository.findByHubId("hub-1")).thenReturn(List.of(scenario));
    }
}
