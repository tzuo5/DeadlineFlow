package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Task;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyGraphService {

    public TopologyResult topologicalSort(Collection<Task> tasks, Collection<Dependency> dependencies) {
        int taskCount = tasks.size();
        Map<String, Task> taskById = new HashMap<>(Math.max(16, taskCount * 2));
        for (Task task : tasks) {
            taskById.put(task.id(), task);
        }

        Map<String, Set<String>> adjacency = new HashMap<>(Math.max(16, taskCount * 2));
        Map<String, Integer> indegree = new HashMap<>(Math.max(16, taskCount * 2));
        for (Task task : tasks) {
            adjacency.put(task.id(), new HashSet<>());
            indegree.put(task.id(), 0);
        }

        for (Dependency dependency : dependencies) {
            if (!taskById.containsKey(dependency.fromTaskId()) || !taskById.containsKey(dependency.toTaskId())) {
                continue;
            }
            Set<String> neighbors = adjacency.get(dependency.fromTaskId());
            if (neighbors.add(dependency.toTaskId())) {
                indegree.put(dependency.toTaskId(), indegree.get(dependency.toTaskId()) + 1);
            }
        }

        ArrayDeque<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> ordered = new ArrayList<>(taskCount);
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            ordered.add(current);

            Set<String> nextNodes = adjacency.get(current);
            if (nextNodes == null || nextNodes.isEmpty()) {
                continue;
            }
            for (String next : nextNodes) {
                int nextIndegree = indegree.get(next) - 1;
                indegree.put(next, nextIndegree);
                if (nextIndegree == 0) {
                    queue.add(next);
                }
            }
        }

        boolean hasCycle = ordered.size() != taskById.size();
        Set<String> cycleNodes = new HashSet<>();
        if (hasCycle) {
            for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
                if (entry.getValue() > 0) {
                    cycleNodes.add(entry.getKey());
                }
            }
        }
        return new TopologyResult(ordered, hasCycle, cycleNodes);
    }

    public boolean createsCycle(Collection<Task> tasks, Collection<Dependency> dependencies, Dependency candidate) {
        List<Dependency> all = new ArrayList<>(dependencies.size() + 1);
        all.addAll(dependencies);
        all.add(candidate);
        return topologicalSort(tasks, all).hasCycle();
    }

    public record TopologyResult(List<String> orderedTaskIds, boolean hasCycle, Set<String> cycleTaskIds) {
    }
}
