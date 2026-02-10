package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Task;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CriticalPathService {
    private final DependencyGraphService dependencyGraphService;

    public CriticalPathService(DependencyGraphService dependencyGraphService) {
        this.dependencyGraphService = dependencyGraphService;
    }

    public CriticalPathResult compute(Collection<Task> tasks, Collection<Dependency> dependencies) {
        if (tasks.isEmpty()) {
            return CriticalPathResult.empty();
        }

        DependencyGraphService.TopologyResult topologyResult = dependencyGraphService.topologicalSort(tasks, dependencies);
        if (topologyResult.hasCycle()) {
            return CriticalPathResult.cycle(topologyResult.cycleTaskIds());
        }

        int taskCount = tasks.size();
        Map<String, Task> taskById = new HashMap<>(Math.max(16, taskCount * 2));
        LocalDate projectStart = null;
        for (Task task : tasks) {
            taskById.put(task.id(), task);
            if (projectStart == null || task.startDate().isBefore(projectStart)) {
                projectStart = task.startDate();
            }
        }
        if (projectStart == null) {
            return CriticalPathResult.empty();
        }

        Map<String, List<String>> predecessors = new HashMap<>(Math.max(16, taskCount * 2));
        Map<String, List<String>> successors = new HashMap<>(Math.max(16, taskCount * 2));
        for (Task task : tasks) {
            predecessors.put(task.id(), new ArrayList<>());
            successors.put(task.id(), new ArrayList<>());
        }

        for (Dependency dependency : dependencies) {
            if (!taskById.containsKey(dependency.fromTaskId()) || !taskById.containsKey(dependency.toTaskId())) {
                continue;
            }
            predecessors.get(dependency.toTaskId()).add(dependency.fromTaskId());
            successors.get(dependency.fromTaskId()).add(dependency.toTaskId());
        }

        Map<String, Integer> earliestStart = new HashMap<>(Math.max(16, taskCount * 2));
        Map<String, Integer> earliestFinish = new HashMap<>(Math.max(16, taskCount * 2));
        int projectFinishOffset = 0;

        for (String taskId : topologyResult.orderedTaskIds()) {
            Task task = taskById.get(taskId);
            int duration = Math.toIntExact(task.durationDaysInclusive());
            int baseStart = Math.toIntExact(ChronoUnit.DAYS.between(projectStart, task.startDate()));

            int earliest = baseStart;
            for (String predecessorId : predecessors.get(taskId)) {
                earliest = Math.max(earliest, earliestFinish.get(predecessorId) + 1);
            }

            int finish = earliest + duration - 1;
            earliestStart.put(taskId, earliest);
            earliestFinish.put(taskId, finish);
            if (finish > projectFinishOffset) {
                projectFinishOffset = finish;
            }
        }

        Map<String, Integer> latestStart = new HashMap<>(Math.max(16, taskCount * 2));
        Map<String, Integer> latestFinish = new HashMap<>(Math.max(16, taskCount * 2));
        List<String> reverseOrder = new ArrayList<>(topologyResult.orderedTaskIds());
        java.util.Collections.reverse(reverseOrder);

        for (String taskId : reverseOrder) {
            Task task = taskById.get(taskId);
            int duration = Math.toIntExact(task.durationDaysInclusive());

            int lf;
            List<String> next = successors.get(taskId);
            if (next.isEmpty()) {
                lf = projectFinishOffset;
            } else {
                int minLatestStartMinusOne = Integer.MAX_VALUE;
                for (String successorId : next) {
                    Integer successorLatestStart = latestStart.get(successorId);
                    if (successorLatestStart == null) {
                        continue;
                    }
                    minLatestStartMinusOne = Math.min(minLatestStartMinusOne, successorLatestStart - 1);
                }
                lf = minLatestStartMinusOne == Integer.MAX_VALUE ? projectFinishOffset : minLatestStartMinusOne;
            }

            int ls = lf - duration + 1;
            latestFinish.put(taskId, lf);
            latestStart.put(taskId, ls);
        }

        Map<String, Integer> slackDays = new HashMap<>(Math.max(16, taskCount * 2));
        Set<String> criticalTaskIds = new HashSet<>(Math.max(16, taskCount * 2));
        for (String taskId : topologyResult.orderedTaskIds()) {
            int slack = latestStart.get(taskId) - earliestStart.get(taskId);
            slackDays.put(taskId, slack);
            if (slack == 0) {
                criticalTaskIds.add(taskId);
            }
        }

        LocalDate finishDate = projectStart.plusDays(projectFinishOffset);

        return new CriticalPathResult(
                false,
                Set.of(),
                finishDate,
                earliestStart,
                earliestFinish,
                latestStart,
                latestFinish,
                slackDays,
                criticalTaskIds
        );
    }
}
