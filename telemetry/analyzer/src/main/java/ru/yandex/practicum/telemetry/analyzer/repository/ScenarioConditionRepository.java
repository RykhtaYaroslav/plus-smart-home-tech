package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioCondition;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioConditionId;

import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {
    @Query("select distinct link.id.conditionId from ScenarioCondition link where link.id.scenarioId = :scenarioId")
    List<Long> findConditionIdsByScenarioId(@Param("scenarioId") Long scenarioId);

    @Modifying
    @Query("delete from ScenarioCondition where id.scenarioId = :scenarioId")
    void deleteAllByScenarioId(@Param("scenarioId") Long scenarioId);
}
