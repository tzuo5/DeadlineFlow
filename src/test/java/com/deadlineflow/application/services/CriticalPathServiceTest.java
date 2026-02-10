package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriticalPathServiceTest {

    private final CriticalPathService criticalPathService = new CriticalPathService(new DependencyGraphService());

    @Test
    void computesEarliestLatestAndSlack() {
        Task a = new Task("a", 1, "A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), 0, "TODO"); // 3d
        Task b = new Task("b", 1, "B", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 0, "TODO"); // 2d
        Task c = new Task("c", 1, "C", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 4), 0, "TODO"); // 4d
        Task d = new Task("d", 1, "D", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 0, "TODO"); // 2d

        List<Dependency> dependencies = List.of(
                new Dependency("d1", "a", "c", DependencyType.FINISH_START),
                new Dependency("d2", "b", "c", DependencyType.FINISH_START),
                new Dependency("d3", "c", "d", DependencyType.FINISH_START)
        );

        CriticalPathResult result = criticalPathService.compute(List.of(a, b, c, d), dependencies);

        assertFalse(result.hasCycle());
        assertEquals(0, result.slackDays().get("a"));
        assertEquals(1, result.slackDays().get("b"));
        assertEquals(0, result.slackDays().get("c"));
        assertEquals(0, result.slackDays().get("d"));

        assertTrue(result.criticalTaskIds().contains("a"));
        assertTrue(result.criticalTaskIds().contains("c"));
        assertTrue(result.criticalTaskIds().contains("d"));
        assertEquals(LocalDate.of(2026, 1, 9), result.projectFinishDate());
    }

    @Test
    void cycleDisablesCriticalPath() {
        Task a = new Task("a", 1, "A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 0, "TODO");
        Task b = new Task("b", 1, "B", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), 0, "TODO");

        List<Dependency> dependencies = List.of(
                new Dependency("d1", "a", "b", DependencyType.FINISH_START),
                new Dependency("d2", "b", "a", DependencyType.FINISH_START)
        );

        CriticalPathResult result = criticalPathService.compute(List.of(a, b), dependencies);

        assertTrue(result.hasCycle());
        assertTrue(result.projectFinishDate() == null);
        assertTrue(result.criticalTaskIds().isEmpty());
    }
}
