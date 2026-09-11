package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioAction;
import ru.yandex.practicum.telemetry.analyzer.model.ScenarioActionId;

import java.util.List;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, ScenarioActionId> {
    @Query("select distinct link.id.actionId from ScenarioAction link where link.id.scenarioId = :scenarioId")
    List<Long> findActionIdsByScenarioId(@Param("scenarioId") Long scenarioId);

    @Modifying
    @Query("delete from ScenarioAction where id.scenarioId = :scenarioId")
    void deleteAllByScenarioId(@Param("scenarioId") Long scenarioId);
}
