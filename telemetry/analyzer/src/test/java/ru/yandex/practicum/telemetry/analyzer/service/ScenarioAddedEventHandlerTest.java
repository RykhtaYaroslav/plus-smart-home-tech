package ru.yandex.practicum.telemetry.analyzer.service;

import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;
import ru.yandex.practicum.telemetry.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.telemetry.analyzer.service.hub.ScenarioAddedEventHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(showSql = false, properties = {
        "spring.datasource.url=jdbc:h2:mem:scenario-handler;MODE=PostgreSQL;NON_KEYWORDS=VALUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ScenarioAddedEventHandlerTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ScenarioAddedEventHandlerTest {
    private static final String HUB_ID = "hub-1";
    private static final String SCENARIO_NAME = "climate";

    @Autowired
    private ScenarioAddedEventHandler handler;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private SqlInspector sqlInspector;
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
        jdbc.update("insert into sensors (id, hub_id) values (?, ?)", "sensor-1", HUB_ID);
        jdbc.update("insert into sensors (id, hub_id) values (?, ?)", "sensor-2", HUB_ID);
        jdbc.update("insert into sensors (id, hub_id) values (?, ?)", "foreign-sensor", "hub-2");
        sqlInspector.statements.clear();
    }

    @Test
    void savesScenarioAndLinksWithAllConditionValueVariants() {
        handler.handle(event(List.of(
                condition("sensor-1", ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.GREATER_THAN, -10),
                condition("sensor-1", ConditionTypeAvro.MOTION, ConditionOperationAvro.EQUALS, true),
                condition("sensor-2", ConditionTypeAvro.SWITCH, ConditionOperationAvro.EQUALS, false),
                condition("sensor-2", ConditionTypeAvro.HUMIDITY, ConditionOperationAvro.LOWER_THAN, null)
        ), List.of(
                action("sensor-1", ActionTypeAvro.ACTIVATE, null),
                action("sensor-1", ActionTypeAvro.DEACTIVATE, null),
                action("sensor-2", ActionTypeAvro.INVERSE, null),
                action("sensor-2", ActionTypeAvro.SET_VALUE, 25)
        )));

        assertOneSensorLookup();
        Long scenarioId = scenarioId();
        assertThat(scenarioId).isPositive();
        assertThat(jdbc.queryForList("select hub_id, name from scenarios"))
                .singleElement().satisfies(row -> {
                    assertThat(row.get("hub_id")).isEqualTo(HUB_ID);
                    assertThat(row.get("name")).isEqualTo(SCENARIO_NAME);
                });
        assertThat(jdbc.query("""
                select sc.scenario_id, sc.sensor_id, c.type, c.operation, c.value
                from scenario_conditions sc join conditions c on c.id = sc.condition_id
                """, (rs, row) -> tuple(rs.getLong("scenario_id"), rs.getString("sensor_id"),
                rs.getString("type"), rs.getString("operation"), rs.getObject("value", Integer.class))))
                .containsExactlyInAnyOrder(
                        tuple(scenarioId, "sensor-1", "TEMPERATURE", "GREATER_THAN", -10),
                        tuple(scenarioId, "sensor-1", "MOTION", "EQUALS", 1),
                        tuple(scenarioId, "sensor-2", "SWITCH", "EQUALS", 0),
                        tuple(scenarioId, "sensor-2", "HUMIDITY", "LOWER_THAN", null));
        assertThat(jdbc.query("""
                select sa.scenario_id, sa.sensor_id, a.type, a.value
                from scenario_actions sa join actions a on a.id = sa.action_id
                """, (rs, row) -> tuple(rs.getLong("scenario_id"), rs.getString("sensor_id"),
                rs.getString("type"), rs.getObject("value", Integer.class))))
                .containsExactlyInAnyOrder(
                        tuple(scenarioId, "sensor-1", "ACTIVATE", null),
                        tuple(scenarioId, "sensor-1", "DEACTIVATE", null),
                        tuple(scenarioId, "sensor-2", "INVERSE", null),
                        tuple(scenarioId, "sensor-2", "SET_VALUE", 25));
    }

    @Test
    void repeatedEventKeepsScenarioIdAndDoesNotDuplicateLinks() {
        HubEventAvro event = simpleEvent("sensor-1", 20);
        handler.handle(event);
        Long originalId = scenarioId();
        Long oldConditionId = jdbc.queryForObject("select id from conditions", Long.class);
        Long oldActionId = jdbc.queryForObject("select id from actions", Long.class);
        sqlInspector.statements.clear();

        handler.handle(event);

        assertOneSensorLookup();
        assertThat(scenarioId()).isEqualTo(originalId);
        assertThat(count("scenarios")).isEqualTo(1);
        assertThat(count("scenario_conditions")).isEqualTo(1);
        assertThat(count("scenario_actions")).isEqualTo(1);
        assertThat(count("conditions")).isEqualTo(1);
        assertThat(count("actions")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select id from conditions", Long.class)).isNotEqualTo(oldConditionId);
        assertThat(jdbc.queryForObject("select id from actions", Long.class)).isNotEqualTo(oldActionId);
        assertDeletionOrder();
        assertThat(linkedConditionValues(originalId)).containsExactly(20);
        assertThat(linkedActionValues(originalId)).containsExactly(20);
    }

    @Test
    void replacesOldLinksAndPreservesOtherScenarioAndSharedEntities() {
        handler.handle(simpleEvent("sensor-1", 20));
        Long originalId = scenarioId();
        jdbc.update("insert into scenarios (hub_id, name) values (?, ?)", HUB_ID, "other-scenario");
        Long otherId = jdbc.queryForObject("select id from scenarios where name = ?", Long.class, "other-scenario");
        jdbc.update("""
                insert into scenario_conditions (scenario_id, sensor_id, condition_id)
                select ?, sensor_id, condition_id from scenario_conditions where scenario_id = ?
                """, otherId, originalId);
        jdbc.update("""
                insert into scenario_actions (scenario_id, sensor_id, action_id)
                select ?, sensor_id, action_id from scenario_actions where scenario_id = ?
                """, otherId, originalId);
        sqlInspector.statements.clear();

        handler.handle(simpleEvent("sensor-2", 35));

        assertOneSensorLookup();
        assertThat(scenarioId()).isEqualTo(originalId);
        assertThat(count("scenarios")).isEqualTo(2);
        assertThat(count("scenario_conditions")).isEqualTo(2);
        assertThat(count("scenario_actions")).isEqualTo(2);
        assertThat(count("conditions")).isEqualTo(2);
        assertThat(count("actions")).isEqualTo(2);
        assertThat(jdbc.queryForList("select sensor_id from scenario_conditions where scenario_id = ?",
                String.class, originalId)).containsExactly("sensor-2");
        assertThat(jdbc.queryForList("select sensor_id from scenario_actions where scenario_id = ?",
                String.class, originalId)).containsExactly("sensor-2");
        assertThat(linkedConditionValues(originalId)).containsExactly(35);
        assertThat(linkedActionValues(originalId)).containsExactly(35);
        assertThat(linkedConditionValues(otherId)).containsExactly(20);
        assertThat(linkedActionValues(otherId)).containsExactly(20);
    }

    @Test
    void emptyEventRemovesAllOldLinks() {
        handler.handle(simpleEvent("sensor-1", 20));
        Long originalId = scenarioId();
        sqlInspector.statements.clear();

        handler.handle(event(List.of(), List.of()));

        assertOneSensorLookup();
        assertThat(scenarioId()).isEqualTo(originalId);
        assertThat(count("scenario_conditions")).isZero();
        assertThat(count("scenario_actions")).isZero();
        assertThat(count("conditions")).isZero();
        assertThat(count("actions")).isZero();
        assertDeletionOrder();

        // Replacing an already empty scenario must also handle empty lists of old IDs.
        handler.handle(event(List.of(), List.of()));
        assertThat(count("scenarios")).isEqualTo(1);
        assertThat(count("conditions")).isZero();
        assertThat(count("actions")).isZero();
    }

    @Test
    void replacementPreservesManagedCollectionsAndRemovesUnsharedOldEntities() {
        handler.handle(simpleEvent("sensor-1", 20));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Scenario scenario = scenarioRepository.findByHubIdAndName(HUB_ID, SCENARIO_NAME).orElseThrow();
            var conditions = scenario.getConditions();
            var actions = scenario.getActions();
            assertThat(conditions).hasSize(1);
            assertThat(actions).hasSize(1);

            handler.handle(simpleEvent("sensor-2", 35));

            assertThat(scenario.getConditions()).isSameAs(conditions)
                    .singleElement().satisfies(link -> assertThat(link.getCondition().getValue()).isEqualTo(35));
            assertThat(scenario.getActions()).isSameAs(actions)
                    .singleElement().satisfies(link -> assertThat(link.getAction().getValue()).isEqualTo(35));
        });

        assertThat(count("conditions")).isEqualTo(1);
        assertThat(count("actions")).isEqualTo(1);
        assertThat(linkedConditionValues(scenarioId())).containsExactly(35);
        assertThat(linkedActionValues(scenarioId())).containsExactly(35);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 20})
    void replacementUsesConstantNumberOfSelectsRegardlessOfSensorCount(int sensorCount) {
        List<ScenarioConditionAvro> conditions = new ArrayList<>();
        List<DeviceActionAvro> actions = new ArrayList<>();
        for (int i = 0; i < sensorCount; i++) {
            String sensorId = "bulk-sensor-" + i;
            jdbc.update("insert into sensors (id, hub_id) values (?, ?)", sensorId, HUB_ID);
            conditions.add(condition(sensorId, ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.EQUALS, i));
            actions.add(action(sensorId, ActionTypeAvro.SET_VALUE, i));
        }
        HubEventAvro event = event(conditions, actions);
        handler.handle(event);
        sqlInspector.statements.clear();

        handler.handle(event);

        assertOneSensorLookup();
        // Scenario + sensors + two ID projections + two collection initializations, independent of N.
        assertThat(sqlInspector.statements).filteredOn(sql -> sql.startsWith("select ")).hasSize(6);
        assertThat(count("conditions")).isEqualTo(sensorCount);
        assertThat(count("actions")).isEqualTo(sensorCount);
        assertThat(count("scenario_conditions")).isEqualTo(sensorCount);
        assertThat(count("scenario_actions")).isEqualTo(sensorCount);
    }

    @ParameterizedTest
    @MethodSource("unsupportedConditionValues")
    void unsupportedConditionValueReportsItsTypeAndRollsBackReplacement(Object value) {
        handler.handle(simpleEvent("sensor-1", 20));
        Map<String, List<Map<String, Object>>> before = databaseState();
        sqlInspector.statements.clear();

        assertThatThrownBy(() -> handler.handle(event(List.of(
                condition("sensor-2", ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.EQUALS, 35),
                condition("sensor-2", ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.EQUALS, value)
        ), List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("тип значения условия")
                .hasMessageContaining(value.getClass().getName());

        assertDeletionOrder();
        assertThat(sqlInspector.statements).anyMatch(sql -> sql.startsWith("insert into conditions"));
        assertThat(databaseState()).isEqualTo(before);
    }

    static Stream<Object> unsupportedConditionValues() {
        return Stream.of("35", 35L, 35.0, new Object());
    }

    @ParameterizedTest
    @ValueSource(strings = {"scenario_conditions", "scenario_actions"})
    void linkConstraintFailureRollsBackReplacement(String table) {
        handler.handle(simpleEvent("sensor-1", 20));
        Map<String, List<Map<String, Object>>> before = databaseState();
        jdbc.execute("alter table " + table + " add constraint reject_sensor_2 check (sensor_id <> 'sensor-2')");
        sqlInspector.statements.clear();
        try {
            assertThatThrownBy(() -> handler.handle(simpleEvent("sensor-2", 35)))
                    .isInstanceOf(RuntimeException.class)
                    .hasStackTraceContaining("REJECT_SENSOR_2");

            assertDeletionOrder();
            assertThat(databaseState()).isEqualTo(before);
        } finally {
            jdbc.execute("alter table " + table + " drop constraint reject_sensor_2");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-sensor", "foreign-sensor"})
    void rejectsMissingOrForeignActionSensorBeforeSavingNewScenario(String sensorId) {
        Map<String, List<Map<String, Object>>> before = databaseState();

        assertThatThrownBy(() -> handler.handle(event(
                List.of(condition("sensor-1", ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.EQUALS, 20)),
                List.of(action(sensorId, ActionTypeAvro.SET_VALUE, 25)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(HUB_ID)
                .hasMessageContaining(sensorId);

        assertOneSensorLookup();
        assertThat(databaseState()).isEqualTo(before);
    }

    @Test
    void missingConditionSensorLeavesExistingScenarioUnchanged() {
        handler.handle(simpleEvent("sensor-1", 20));
        Map<String, List<Map<String, Object>>> before = databaseState();
        sqlInspector.statements.clear();

        assertThatThrownBy(() -> handler.handle(event(
                List.of(condition("missing-sensor", ConditionTypeAvro.MOTION, ConditionOperationAvro.EQUALS, true)),
                List.of(action("sensor-2", ActionTypeAvro.ACTIVATE, null)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing-sensor");

        assertOneSensorLookup();
        assertThat(databaseState()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failureAfterPartialWritesRollsBackCreationOrReplacement(boolean existingScenario) {
        if (existingScenario) {
            handler.handle(simpleEvent("sensor-1", 20));
        }
        Map<String, List<Map<String, Object>>> before = databaseState();
        sqlInspector.statements.clear();
        DeviceActionAvro invalidAction = action("sensor-2", null, 30);

        assertThatThrownBy(() -> handler.handle(event(
                List.of(condition("sensor-2", ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.EQUALS, 30)),
                List.of(action("sensor-2", ActionTypeAvro.SET_VALUE, 30), invalidAction))))
                .isInstanceOf(NullPointerException.class);

        assertThat(sqlInspector.statements).anyMatch(sql -> sql.startsWith("insert into conditions"));
        assertThat(sqlInspector.statements).anyMatch(sql -> sql.startsWith("insert into actions"));
        if (existingScenario) {
            assertDeletionOrder();
        }
        assertThat(databaseState()).isEqualTo(before);
    }

    private HubEventAvro simpleEvent(String sensorId, int value) {
        return event(List.of(condition(sensorId, ConditionTypeAvro.TEMPERATURE, ConditionOperationAvro.EQUALS, value)),
                List.of(action(sensorId, ActionTypeAvro.SET_VALUE, value)));
    }

    private HubEventAvro event(List<ScenarioConditionAvro> conditions, List<DeviceActionAvro> actions) {
        return new HubEventAvro(HUB_ID, Instant.EPOCH, new ScenarioAddedEventAvro(SCENARIO_NAME, conditions, actions));
    }

    private ScenarioConditionAvro condition(String sensorId, ConditionTypeAvro type,
                                           ConditionOperationAvro operation, Object value) {
        return new ScenarioConditionAvro(sensorId, type, operation, value);
    }

    private DeviceActionAvro action(String sensorId, ActionTypeAvro type, Integer value) {
        return new DeviceActionAvro(sensorId, type, value);
    }

    private Long scenarioId() {
        return jdbc.queryForObject("select id from scenarios where hub_id = ? and name = ?",
                Long.class, HUB_ID, SCENARIO_NAME);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private List<Integer> linkedConditionValues(Long scenarioId) {
        return jdbc.queryForList("""
                select c.value from conditions c
                join scenario_conditions sc on sc.condition_id = c.id where sc.scenario_id = ?
                """, Integer.class, scenarioId);
    }

    private List<Integer> linkedActionValues(Long scenarioId) {
        return jdbc.queryForList("""
                select a.value from actions a
                join scenario_actions sa on sa.action_id = a.id where sa.scenario_id = ?
                """, Integer.class, scenarioId);
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

    private void assertOneSensorLookup() {
        assertThat(sqlInspector.statements)
                .filteredOn(sql -> sql.contains(" from sensors "))
                .singleElement()
                .satisfies(sql -> assertThat(sql).contains("hub_id", " in "));

        // Initializing the two managed collections may join sensors, but must not cause per-sensor SELECTs.
        assertThat(sqlInspector.statements)
                .filteredOn(sql -> sql.contains(" join sensors "))
                .hasSizeLessThanOrEqualTo(2);
    }

    private void assertDeletionOrder() {
        assertThat(sqlInspector.statements)
                .filteredOn(sql -> sql.startsWith("delete from "))
                .extracting(sql -> sql.split(" ")[2])
                .containsExactly("scenario_conditions", "scenario_actions", "conditions", "actions");
    }

    static class SqlInspector implements StatementInspector {
        private final List<String> statements = new ArrayList<>();

        @Override
        public String inspect(String sql) {
            statements.add(sql.toLowerCase(Locale.ROOT));
            return sql;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableJpaRepositories(basePackageClasses = ScenarioRepository.class)
    @EntityScan(basePackageClasses = Scenario.class)
    @Import(ScenarioAddedEventHandler.class)
    static class Config {
        @Bean
        SqlInspector sqlInspector() {
            return new SqlInspector();
        }

        @Bean
        HibernatePropertiesCustomizer sqlInspectorCustomizer(SqlInspector sqlInspector) {
            return properties -> properties.put("hibernate.session_factory.statement_inspector", sqlInspector);
        }
    }
}
