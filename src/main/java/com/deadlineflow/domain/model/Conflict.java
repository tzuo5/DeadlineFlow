package com.deadlineflow.domain.model;

import java.util.Objects;

public final class Conflict {
    private final String dependencyId;
    private final String fromTaskId;
    private final String toTaskId;
    private final String message;

    public Conflict(String dependencyId, String fromTaskId, String toTaskId, String message) {
        this.dependencyId = Objects.requireNonNull(dependencyId, "dependencyId");
        this.fromTaskId = Objects.requireNonNull(fromTaskId, "fromTaskId");
        this.toTaskId = Objects.requireNonNull(toTaskId, "toTaskId");
        this.message = Objects.requireNonNull(message, "message");
    }

    public String dependencyId() {
        return dependencyId;
    }

    public String fromTaskId() {
        return fromTaskId;
    }

    public String toTaskId() {
        return toTaskId;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
