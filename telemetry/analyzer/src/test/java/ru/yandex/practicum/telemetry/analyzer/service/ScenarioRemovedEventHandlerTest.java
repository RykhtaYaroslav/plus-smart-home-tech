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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.hub.ScenarioRemovedEventHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:scenario-removal;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ScenarioRemovedEventHandlerTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ScenarioRemovedEventHandlerTest {
    @Autowired
    private ScenarioRemovedEventHandler handler;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ScenarioRepository scenarioRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        for (String table : List.of("scenario_conditions", "scenario_actions", "conditions", "actions",
                "scenarios", "sensors")) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("insert into sensors (id, hub_id) values ('sensor-1', 'hub-1'), ('sensor-2', 'hub-2')");
        jdbc.update("insert into scenarios (id, hub_id, name) values (1, 'hub-1', 'climate')");
        jdbc.update("insert into conditions (id, type, operation, value) values (1, 'TEMPERATURE', 'EQUALS', 20)");
        jdbc.update("insert into actions (id, type, value) values (1, 'SET_VALUE', 25)");
        addLinks(1, "sensor-1", 1);
    }

    @Test
    void removesScenarioAndDetailsButPreservesSensors() {
        assertThat(handler.getPayloadType()).isEqualTo(ScenarioRemovedEventAvro.class);

        handler.handle(event("hub-1", "climate"));

        for (String table : List.of("scenarios", "scenario_conditions", "scenario_actions", "conditions", "actions")) {
            assertThat(count(table)).as(table).isZero();
        }
        assertThat(count("sensors")).isEqualTo(2);
    }

    @Test
    void preservesOtherScenariosAndTheirDetails() {
        jdbc.update("""
                insert into scenarios (id, hub_id, name)
                values (2, 'hub-2', 'climate'), (3, 'hub-1', 'other')
                """);
        jdbc.update("insert into conditions (id, type, operation, value) values (2, 'MOTION', 'EQUALS', 1)");
        jdbc.update("insert into actions (id, type, value) values (2, 'ACTIVATE', null)");
        addLinks(2, "sensor-2", 2);
        addLinks(3, "sensor-1", 2);

        handler.handle(event("hub-1", "climate"));

        assertThat(jdbc.queryForList("select id from scenarios order by id", Long.class)).containsExactly(2L, 3L);
        assertThat(jdbc.queryForList("select id from conditions", Long.class)).containsExactly(2L);
        assertThat(jdbc.queryForList("select id from actions", Long.class)).containsExactly(2L);
        assertThat(count("scenario_conditions")).isEqualTo(2);
        assertThat(count("scenario_actions")).isEqualTo(2);
        assertThat(count("sensors")).isEqualTo(2);
    }

    @Test
    void preservesSharedDetailsUntilLastScenarioIsRemoved() {
        jdbc.update("insert into scenarios (id, hub_id, name) values (2, 'hub-1', 'other')");
        addLinks(2, "sensor-1", 1);

        handler.handle(event("hub-1", "climate"));

        assertThat(jdbc.queryForList("select scenario_id from scenario_conditions", Long.class)).containsExactly(2L);
        assertThat(jdbc.queryForList("select scenario_id from scenario_actions", Long.class)).containsExactly(2L);
        assertThat(count("conditions")).isEqualTo(1);
        assertThat(count("actions")).isEqualTo(1);

        handler.handle(event("hub-1", "other"));

        assertThat(count("conditions")).isZero();
        assertThat(count("actions")).isZero();
    }

    @Test
    void missingScenarioOrDifferentHubLeavesDatabaseUnchanged() {
        var before = databaseState();

        handler.handle(event("hub-1", "missing"));
        handler.handle(event("hub-2", "climate"));

        assertThat(databaseState()).isEqualTo(before);
    }

    @Test
    void removesEmptyScenarioAndIgnoresRepeatedEvent() {
        jdbc.update("insert into scenarios (id, hub_id, name) values (2, 'hub-1', 'empty')");

        handler.handle(event("hub-1", "empty"));
        var afterRemoval = databaseState();
        handler.handle(event("hub-1", "empty"));

        assertThat(jdbc.queryForList("select id from scenarios", Long.class)).containsExactly(1L);
        assertThat(databaseState()).isEqualTo(afterRemoval);
    }

    @Test
    void removesScenarioWithInitializedCollections() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Scenario scenario = scenarioRepository.findById(1L).orElseThrow();
            assertThat(scenario.getConditions()).hasSize(1);
            assertThat(scenario.getActions()).hasSize(1);

            handler.handle(event("hub-1", "climate"));

            assertThat(scenario.getConditions()).isEmpty();
            assertThat(scenario.getActions()).isEmpty();
        });

        assertThat(count("scenarios")).isZero();
        assertThat(count("conditions")).isZero();
        assertThat(count("actions")).isZero();
    }

    @Test
    void scenarioDeletionFailureRollsBackDetailDeletion() {
        var before = databaseState();
        jdbc.execute("create table scenario_references (scenario_id bigint references scenarios(id))");
        try {
            jdbc.update("insert into scenario_references (scenario_id) values (1)");

            assertThatThrownBy(() -> handler.handle(event("hub-1", "climate")))
                    .isInstanceOf(RuntimeException.class);

            assertThat(databaseState()).isEqualTo(before);
        } finally {
            jdbc.execute("drop table scenario_references");
        }
    }

    private HubEventAvro event(String hubId, String name) {
        return new HubEventAvro(hubId, Instant.EPOCH, new ScenarioRemovedEventAvro(name));
    }

    private void addLinks(long scenarioId, String sensorId, long detailId) {
        jdbc.update("insert into scenario_conditions (scenario_id, sensor_id, condition_id) values (?, ?, ?)",
                scenarioId, sensorId, detailId);
        jdbc.update("insert into scenario_actions (scenario_id, sensor_id, action_id) values (?, ?, ?)",
                scenarioId, sensorId, detailId);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private Map<String, List<Map<String, Object>>> databaseState() {
        Map<String, List<Map<String, Object>>> state = new LinkedHashMap<>();
        for (String table : List.of("scenarios", "sensors", "conditions", "actions")) {
            state.put(table, jdbc.queryForList("select * from " + table + " order by id"));
        }
        for (String table : List.of("scenario_conditions", "scenario_actions")) {
            state.put(table, jdbc.queryForList("select * from " + table + " order by 1, 2, 3"));
        }
        return state;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = ScenarioRepository.class)
    @EntityScan(basePackageClasses = Scenario.class)
    @Import(ScenarioRemovedEventHandler.class)
    static class Config {
    }
}
