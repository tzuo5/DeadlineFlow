package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.Task;
import javafx.collections.ObservableList;

import java.util.Optional;

public interface TaskRepository {
    ObservableList<Task> getAll();

    Optional<Task> findById(String taskId);

    Task save(Task task);

    void delete(String taskId);
}
