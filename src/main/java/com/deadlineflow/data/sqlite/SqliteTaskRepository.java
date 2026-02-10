package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.domain.model.Task;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class SqliteTaskRepository implements TaskRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteTaskRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<Task> getAll() {
        return workspaceStore.tasks();
    }

    @Override
    public Optional<Task> findById(String taskId) {
        return workspaceStore.tasks().stream().filter(task -> task.id().equals(taskId)).findFirst();
    }

    @Override
    public Task save(Task task) {
        if (findById(task.id()).isPresent()) {
            return update(task);
        }
        return insert(task);
    }

    @Override
    public void delete(String taskId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            statement.setString(1, taskId);
            statement.executeUpdate();
            workspaceStore.reloadTasks();
            workspaceStore.reloadDependencies();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting task", e);
        }
    }

    private Task insert(Task task) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO tasks(id, project_id, title, description, start_date, due_date, progress, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
             )) {
            statement.setString(1, task.id());
            statement.setLong(2, task.projectId());
            statement.setString(3, task.title());
            statement.setString(4, task.description());
            statement.setString(5, task.startDate().toString());
            statement.setString(6, task.dueDate().toString());
            statement.setInt(7, task.progress());
            statement.setString(8, task.status());
            statement.executeUpdate();
            workspaceStore.reloadTasks();
            return task;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed creating task", e);
        }
    }

    private Task update(Task task) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE tasks SET project_id = ?, title = ?, description = ?, start_date = ?, due_date = ?, progress = ?, status = ? WHERE id = ?"
             )) {
            statement.setLong(1, task.projectId());
            statement.setString(2, task.title());
            statement.setString(3, task.description());
            statement.setString(4, task.startDate().toString());
            statement.setString(5, task.dueDate().toString());
            statement.setInt(6, task.progress());
            statement.setString(7, task.status());
            statement.setString(8, task.id());
            statement.executeUpdate();
            workspaceStore.reloadTasks();
            return task;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed updating task", e);
        }
    }
}
