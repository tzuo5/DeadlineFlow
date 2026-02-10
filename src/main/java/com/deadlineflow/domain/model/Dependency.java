package com.deadlineflow.domain.model;

import java.util.Objects;

public final class Dependency {
    private final String id;
    private final String fromTaskId;
    private final String toTaskId;
    private final DependencyType type;

    public Dependency(String id, String fromTaskId, String toTaskId, DependencyType type) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Dependency id is required");
        }
        if (fromTaskId == null || fromTaskId.isBlank()) {
            throw new IllegalArgumentException("Dependency fromTaskId is required");
        }
        if (toTaskId == null || toTaskId.isBlank()) {
            throw new IllegalArgumentException("Dependency toTaskId is required");
        }
        if (fromTaskId.equals(toTaskId)) {
            throw new IllegalArgumentException("Dependency cannot target the same task");
        }
        if (type == null) {
            throw new IllegalArgumentException("Dependency type is required");
        }
        this.id = id;
        this.fromTaskId = fromTaskId;
        this.toTaskId = toTaskId;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public String fromTaskId() {
        return fromTaskId;
    }

    public String toTaskId() {
        return toTaskId;
    }

    public DependencyType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dependency that)) {
            return false;
        }
        return id.equals(that.id)
                && fromTaskId.equals(that.fromTaskId)
                && toTaskId.equals(that.toTaskId)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fromTaskId, toTaskId, type);
    }

    @Override
    public String toString() {
        return fromTaskId + " -> " + toTaskId;
    }
}
