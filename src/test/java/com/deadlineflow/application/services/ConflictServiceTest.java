package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConflictServiceTest {

    private final ConflictService conflictService = new ConflictService();

    @Test
    void finishStartConflictRequiresOneDayLag() {
        Task predecessor = new Task(
                "a",
                1,
                "Predecessor",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10),
                0,
                "TODO"
        );
        Task dependent = new Task(
                "b",
                1,
                "Dependent",
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 12),
                0,
                "TODO"
        );

        List<Dependency> dependencies = List.of(new Dependency("d1", "a", "b", DependencyType.FINISH_START));

        assertEquals(1, conflictService.detectDependencyConflicts(List.of(predecessor, dependent), dependencies).size());
    }

    @Test
    void validDependencyProducesNoConflict() {
        Task predecessor = new Task(
                "a",
                1,
                "Predecessor",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 10),
                0,
                "TODO"
        );
        Task dependent = new Task(
                "b",
                1,
                "Dependent",
                LocalDate.of(2026, 2, 11),
                LocalDate.of(2026, 2, 13),
                0,
                "TODO"
        );

        List<Dependency> dependencies = List.of(new Dependency("d1", "a", "b", DependencyType.FINISH_START));

        assertEquals(0, conflictService.detectDependencyConflicts(List.of(predecessor, dependent), dependencies).size());
    }
}
