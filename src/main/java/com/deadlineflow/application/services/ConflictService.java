package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.Conflict;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.Task;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConflictService {
    public static final int FINISH_START_LAG_DAYS = 1;

    public List<Conflict> detectDependencyConflicts(Collection<Task> tasks, Collection<Dependency> dependencies) {
        Map<String, Task> taskById = new HashMap<>();
        for (Task task : tasks) {
            taskById.put(task.id(), task);
        }

        List<Conflict> conflicts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Dependency dependency : dependencies) {
            Task fromTask = taskById.get(dependency.fromTaskId());
            Task toTask = taskById.get(dependency.toTaskId());
            if (fromTask == null || toTask == null) {
                continue;
            }

            if (toTask.startDate().isBefore(fromTask.dueDate().plusDays(FINISH_START_LAG_DAYS))) {
                String message = "Dependency violation: '"
                        + toTask.title()
                        + "' starts "
                        + formatter.format(toTask.startDate())
                        + ", must start on or after "
                        + formatter.format(fromTask.dueDate().plusDays(FINISH_START_LAG_DAYS))
                        + " because it depends on '"
                        + fromTask.title()
                        + "'.";
                conflicts.add(new Conflict(dependency.id(), dependency.fromTaskId(), dependency.toTaskId(), message));
            }
        }
        return conflicts;
    }
}
