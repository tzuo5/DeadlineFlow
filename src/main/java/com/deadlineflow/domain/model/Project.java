package com.deadlineflow.domain.model;

import java.util.Objects;

public final class Project {
    private final long id;
    private final String name;
    private final String color;
    private final int priority;

    public Project(long id, String name, String color, int priority) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Project color is required");
        }
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Project priority must be between 1 and 5");
        }
        this.id = id;
        this.name = name.trim();
        this.color = color.trim();
        this.priority = priority;
    }

    public long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String color() {
        return color;
    }

    public int priority() {
        return priority;
    }

    public Project withId(long newId) {
        return new Project(newId, name, color, priority);
    }

    public Project withName(String newName) {
        return new Project(id, newName, color, priority);
    }

    public Project withColor(String newColor) {
        return new Project(id, name, newColor, priority);
    }

    public Project withPriority(int newPriority) {
        return new Project(id, name, color, newPriority);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project project)) {
            return false;
        }
        return id == project.id
                && priority == project.priority
                && name.equals(project.name)
                && color.equals(project.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, color, priority);
    }

    @Override
    public String toString() {
        return name;
    }
}
