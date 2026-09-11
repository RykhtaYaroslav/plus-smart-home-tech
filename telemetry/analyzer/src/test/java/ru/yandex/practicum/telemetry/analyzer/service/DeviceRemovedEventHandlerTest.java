package ru.yandex.practicum.telemetry.analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.hub.DeviceRemovedEventHandler;
import ru.yandex.practicum.telemetry.analyzer.service.hub.ScenarioRemovalService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:device-removal;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = DeviceRemovedEventHandlerTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeviceRemovedEventHandlerTest {
    @Autowired
    private DeviceRemovedEventHandler handler;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        for (String table : List.of("scenario_conditions", "scenario_actions", "conditions", "actions", "scenarios", "sensors")) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("insert into sensors (id, hub_id) values ('removed', 'hub-1'), ('kept', 'hub-1'), ('foreign', 'hub-2')");
        jdbc.update("""
                insert into scenarios (id, hub_id, name) values
                (1, 'hub-1', 'condition-only'), (2, 'hub-1', 'action-only'),
                (3, 'hub-1', 'both'), (4, 'hub-1', 'unrelated'), (5, 'hub-2', 'foreign')
                """);
        for (long id = 1; id <= 5; id++) {
            jdbc.update("insert into conditions (id, type, operation, value) values (?, 'TEMPERATURE', 'EQUALS', 20)", id);
            jdbc.update("insert into actions (id, type, value) values (?, 'SET_VALUE', 25)", id);
            String conditionSensor = id == 1 || id == 3 ? "removed" : id == 5 ? "foreign" : "kept";
            String actionSensor = id == 2 || id == 3 ? "removed" : id == 5 ? "foreign" : "kept";
            jdbc.update("insert into scenario_conditions (scenario_id, sensor_id, condition_id) values (?, ?, ?)",
                    id, conditionSensor, id);
            jdbc.update("insert into scenario_actions (scenario_id, sensor_id, action_id) values (?, ?, ?)",
                    id, actionSensor, id);
        }
    }

    @Test
    void removesAllDependentScenariosAndOnlyTheirUnusedDetails() {
        assertThat(handler.getPayloadType()).isEqualTo(DeviceRemovedEventAvro.class);
        handler.handle(event("hub-1", "removed"));

        for (String table : List.of("scenarios", "conditions", "actions")) {
            assertThat(jdbc.queryForList("select id from " + table + " order by id", Long.class))
                    .as(table).containsExactly(4L, 5L);
        }
        for (String table : List.of("scenario_conditions", "scenario_actions")) {
            assertThat(jdbc.queryForList("select scenario_id from " + table + " order by scenario_id", Long.class))
                    .as(table).containsExactly(4L, 5L);
        }
        assertThat(jdbc.queryForList("select id from sensors order by id", String.class)).containsExactly("foreign", "kept");
    }

    @Test
    void preservesDetailsSharedWithUnrelatedScenario() {
        jdbc.update("insert into scenario_conditions (scenario_id, sensor_id, condition_id) values (4, 'kept', 1)");
        jdbc.update("insert into scenario_actions (scenario_id, sensor_id, action_id) values (4, 'kept', 1)");

        handler.handle(event("hub-1", "removed"));

        for (String table : List.of("conditions", "actions")) {
            assertThat(jdbc.queryForList("select id from " + table + " order by id", Long.class)).containsExactly(1L, 4L, 5L);
        }
    }

    @Test
    void ignoresMissingDeviceForeignHubAndRepeatedRemoval() {
        var before = databaseState();
        handler.handle(event("hub-1", "missing"));
        handler.handle(event("hub-2", "removed"));
        assertThat(databaseState()).isEqualTo(before);

        handler.handle(event("hub-1", "removed"));
        var after = databaseState();
        handler.handle(event("hub-1", "removed"));
        assertThat(databaseState()).isEqualTo(after);
    }

    @Test
    void removesUnusedDeviceWithoutChangingScenarios() {
        jdbc.update("insert into sensors (id, hub_id) values ('unused', 'hub-1')");
        handler.handle(event("hub-1", "unused"));
        assertThat(jdbc.queryForObject("select count(*) from scenarios", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from sensors", Integer.class)).isEqualTo(3);
    }

    @Test
    void failureDeletingDeviceRollsBackAllScenarioDeletions() {
        var before = databaseState();
        jdbc.execute("create table device_references (sensor_id varchar references sensors(id))");
        try {
            jdbc.update("insert into device_references values ('removed')");
            assertThatThrownBy(() -> handler.handle(event("hub-1", "removed"))).isInstanceOf(RuntimeException.class);
            assertThat(databaseState()).isEqualTo(before);
        } finally {
            jdbc.execute("drop table device_references");
        }
    }

    private HubEventAvro event(String hubId, String sensorId) {
        return new HubEventAvro(hubId, Instant.EPOCH, new DeviceRemovedEventAvro(sensorId));
    }

    private Map<String, List<Map<String, Object>>> databaseState() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : List.of("sensors", "scenarios", "conditions", "actions", "scenario_conditions", "scenario_actions")) {
            String ordering = table.startsWith("scenario_") ? "1, 2, 3" : "id";
            result.put(table, jdbc.queryForList("select * from " + table + " order by " + ordering));
        }
        return result;
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Scenario.class)
    @EnableJpaRepositories(basePackageClasses = ScenarioRepository.class)
    @Import({DeviceRemovedEventHandler.class, ScenarioRemovalService.class})
    static class Config {
    }
}
