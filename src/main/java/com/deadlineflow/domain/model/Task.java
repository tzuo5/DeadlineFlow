package com.deadlineflow.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class Task {
    public static final String DEFAULT_DESCRIPTION = "";
    public static final String DEFAULT_STATUS = "TODO";

    private final String id;
    private final long projectId;
    private final String title;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate dueDate;
    private final int progress;
    private final String status;

    public Task(
            String id,
            long projectId,
            String title,
            LocalDate startDate,
            LocalDate dueDate,
            int progress,
            String status
    ) {
        this(id, projectId, title, DEFAULT_DESCRIPTION, startDate, dueDate, progress, status);
    }

    public Task(
            String id,
            long projectId,
            String title,
            String description,
            LocalDate startDate,
            LocalDate dueDate,
            int progress,
            String status
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Task id is required");
        }
        if (projectId <= 0) {
            throw new IllegalArgumentException("Task projectId must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }
        if (startDate == null || dueDate == null) {
            throw new IllegalArgumentException("Task dates are required");
        }
        if (dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Task dueDate cannot be before startDate");
        }
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Task progress must be between 0 and 100");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Task status is required");
        }
        this.id = id;
        this.projectId = projectId;
        this.title = title.trim();
        this.description = description == null ? "" : description;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.progress = progress;
        this.status = status.trim();
    }

    public String id() {
        return id;
    }

    public long projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public LocalDate startDate() {
        return startDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public int progress() {
        return progress;
    }

    public String status() {
        return status;
    }

    public long durationDaysInclusive() {
        return ChronoUnit.DAYS.between(startDate, dueDate) + 1;
    }

    public Task withTitle(String newTitle) {
        return new Task(id, projectId, newTitle, description, startDate, dueDate, progress, status);
    }

    public Task withDescription(String newDescription) {
        return new Task(id, projectId, title, newDescription, startDate, dueDate, progress, status);
    }

    public Task withDates(LocalDate newStartDate, LocalDate newDueDate) {
        return new Task(id, projectId, title, description, newStartDate, newDueDate, progress, status);
    }

    public Task withProgress(int newProgress) {
        return new Task(id, projectId, title, description, startDate, dueDate, newProgress, status);
    }

    public Task withStatus(String newStatus) {
        return new Task(id, projectId, title, description, startDate, dueDate, progress, newStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Task task)) {
            return false;
        }
        return projectId == task.projectId
                && progress == task.progress
                && id.equals(task.id)
                && title.equals(task.title)
                && description.equals(task.description)
                && startDate.equals(task.startDate)
                && dueDate.equals(task.dueDate)
                && status.equals(task.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, projectId, title, description, startDate, dueDate, progress, status);
    }

    @Override
    public String toString() {
        return title;
    }
}
