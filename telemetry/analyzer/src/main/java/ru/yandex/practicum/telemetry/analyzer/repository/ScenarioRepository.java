package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.telemetry.analyzer.model.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    List<Scenario> findByHubId(String hubId);
    Optional<Scenario> findByHubIdAndName(String hubId, String name);

    @Query("""
            select s from Scenario s where s.hubId = :hubId and (
                exists (select c from ScenarioCondition c
                        where c.scenario = s and c.sensor.id = :sensorId)
                or exists (select a from ScenarioAction a
                           where a.scenario = s and a.sensor.id = :sensorId)
            )
            """)
    List<Scenario> findAllUsingSensor(@Param("hubId") String hubId, @Param("sensorId") String sensorId);
}
