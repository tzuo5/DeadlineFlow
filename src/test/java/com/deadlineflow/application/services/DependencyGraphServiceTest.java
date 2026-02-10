package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Task;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphServiceTest {

    private final DependencyGraphService graphService = new DependencyGraphService();

    @Test
    void topologicalSortOrdersPredecessorsBeforeSuccessors() {
        Task a = task("a", "A");
        Task b = task("b", "B");
        Task c = task("c", "C");

        List<Dependency> dependencies = List.of(
                dependency("d1", "a", "b"),
                dependency("d2", "b", "c")
        );

        DependencyGraphService.TopologyResult result = graphService.topologicalSort(List.of(a, b, c), dependencies);

        assertFalse(result.hasCycle());
        int idxA = result.orderedTaskIds().indexOf("a");
        int idxB = result.orderedTaskIds().indexOf("b");
        int idxC = result.orderedTaskIds().indexOf("c");
        assertTrue(idxA < idxB);
        assertTrue(idxB < idxC);
    }

    @Test
    void detectsCycle() {
        Task a = task("a", "A");
        Task b = task("b", "B");

        List<Dependency> dependencies = List.of(
                dependency("d1", "a", "b"),
                dependency("d2", "b", "a")
        );

        DependencyGraphService.TopologyResult result = graphService.topologicalSort(List.of(a, b), dependencies);

        assertTrue(result.hasCycle());
        assertTrue(result.cycleTaskIds().contains("a"));
        assertTrue(result.cycleTaskIds().contains("b"));
    }

    private Task task(String id, String title) {
        return new Task(id, 1, title, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), 0, "TODO");
    }

    private Dependency dependency(String id, String from, String to) {
        return new Dependency(id, from, to, DependencyType.FINISH_START);
    }
}
