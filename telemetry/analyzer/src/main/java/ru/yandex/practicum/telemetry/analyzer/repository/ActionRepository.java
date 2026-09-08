package ru.yandex.practicum.telemetry.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.telemetry.analyzer.model.Action;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {
    @Modifying
    @Query("""
            delete from Action a where a.id in :ids
            and not exists (
                select link.id.actionId from ScenarioAction link where link.id.actionId = a.id
            )
            """)
    void deleteUnreferencedByIdIn(@Param("ids") List<Long> ids);
}
