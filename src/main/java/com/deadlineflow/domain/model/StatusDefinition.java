package com.deadlineflow.domain.model;

import java.util.Objects;

public final class StatusDefinition {
    private final String name;
    private final boolean isProtected;

    public StatusDefinition(String name, boolean isProtected) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Status name is required");
        }
        this.name = name.trim();
        this.isProtected = isProtected;
    }

    public String name() {
        return name;
    }

    public boolean isProtected() {
        return isProtected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StatusDefinition that)) {
            return false;
        }
        return isProtected == that.isProtected && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isProtected);
    }

    @Override
    public String toString() {
        return name;
    }
}
