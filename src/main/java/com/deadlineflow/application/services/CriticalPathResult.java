package com.deadlineflow.application.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class CriticalPathResult {
    private final boolean hasCycle;
    private final Set<String> cycleTaskIds;
    private final LocalDate projectFinishDate;
    private final Map<String, Integer> earliestStartDays;
    private final Map<String, Integer> earliestFinishDays;
    private final Map<String, Integer> latestStartDays;
    private final Map<String, Integer> latestFinishDays;
    private final Map<String, Integer> slackDays;
    private final Set<String> criticalTaskIds;

    public CriticalPathResult(
            boolean hasCycle,
            Set<String> cycleTaskIds,
            LocalDate projectFinishDate,
            Map<String, Integer> earliestStartDays,
            Map<String, Integer> earliestFinishDays,
            Map<String, Integer> latestStartDays,
            Map<String, Integer> latestFinishDays,
            Map<String, Integer> slackDays,
            Set<String> criticalTaskIds
    ) {
        this.hasCycle = hasCycle;
        this.cycleTaskIds = Collections.unmodifiableSet(cycleTaskIds);
        this.projectFinishDate = projectFinishDate;
        this.earliestStartDays = Collections.unmodifiableMap(earliestStartDays);
        this.earliestFinishDays = Collections.unmodifiableMap(earliestFinishDays);
        this.latestStartDays = Collections.unmodifiableMap(latestStartDays);
        this.latestFinishDays = Collections.unmodifiableMap(latestFinishDays);
        this.slackDays = Collections.unmodifiableMap(slackDays);
        this.criticalTaskIds = Collections.unmodifiableSet(criticalTaskIds);
    }

    public static CriticalPathResult empty() {
        return new CriticalPathResult(false, Set.of(), null, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
    }

    public static CriticalPathResult cycle(Set<String> cycleTaskIds) {
        return new CriticalPathResult(true, cycleTaskIds, null, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());
    }

    public boolean hasCycle() {
        return hasCycle;
    }

    public Set<String> cycleTaskIds() {
        return cycleTaskIds;
    }

    public LocalDate projectFinishDate() {
        return projectFinishDate;
    }

    public Map<String, Integer> earliestStartDays() {
        return earliestStartDays;
    }

    public Map<String, Integer> earliestFinishDays() {
        return earliestFinishDays;
    }

    public Map<String, Integer> latestStartDays() {
        return latestStartDays;
    }

    public Map<String, Integer> latestFinishDays() {
        return latestFinishDays;
    }

    public Map<String, Integer> slackDays() {
        return slackDays;
    }

    public Set<String> criticalTaskIds() {
        return criticalTaskIds;
    }
}
