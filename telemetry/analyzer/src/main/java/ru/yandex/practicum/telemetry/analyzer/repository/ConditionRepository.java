package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.telemetry.analyzer.model.Condition;

import java.util.List;

public interface ConditionRepository extends JpaRepository<Condition, Long> {
    @Modifying
    @Query("""
            delete from Condition c where c.id in :ids
            and not exists (
                select link.id.conditionId from ScenarioCondition link where link.id.conditionId = c.id
            )
            """)
    void deleteUnreferencedByIdIn(@Param("ids") List<Long> ids);
}
