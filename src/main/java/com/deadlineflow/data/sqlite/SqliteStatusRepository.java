package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.StatusRepository;
import com.deadlineflow.domain.model.StatusDefinition;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteStatusRepository implements StatusRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteStatusRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<StatusDefinition> getAll() {
        return workspaceStore.statuses();
    }

    @Override
    public void add(String name) {
        String normalized = normalize(name);
        if (existsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("Status already exists");
        }

        try (Connection connection = database.openConnection()) {
            // Status definitions are stored in SQLite to persist user-defined status choices across launches.
            int nextOrder = nextDisplayOrder(connection);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO task_statuses(name, display_order, is_protected) VALUES (?, ?, 0)"
            )) {
                statement.setString(1, normalized);
                statement.setInt(2, nextOrder);
                statement.executeUpdate();
            }
            workspaceStore.reloadStatuses();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed adding status", e);
        }
    }

    @Override
    public void rename(String existingName, String newName) {
        String existing = normalize(existingName);
        String replacement = normalize(newName);

        if (existing.equalsIgnoreCase(replacement)) {
            return;
        }
        if (existsIgnoreCase(replacement)) {
            throw new IllegalArgumentException("Status already exists");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (isProtected(connection, existing)) {
                    throw new IllegalArgumentException("Protected status cannot be renamed");
                }

                try (PreparedStatement renameStatus = connection.prepareStatement(
                        "UPDATE task_statuses SET name = ? WHERE lower(name) = lower(?)"
                )) {
                    renameStatus.setString(1, replacement);
                    renameStatus.setString(2, existing);
                    int count = renameStatus.executeUpdate();
                    if (count == 0) {
                        throw new IllegalArgumentException("Status not found");
                    }
                }

                try (PreparedStatement updateTasks = connection.prepareStatement(
                        "UPDATE tasks SET status = ? WHERE lower(status) = lower(?)"
                )) {
                    updateTasks.setString(1, replacement);
                    updateTasks.setString(2, existing);
                    updateTasks.executeUpdate();
                }

                connection.commit();
                workspaceStore.reloadStatuses();
                workspaceStore.reloadTasks();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed renaming status", e);
        }
    }

    @Override
    public void delete(String name, String fallbackName) {
        String target = normalize(name);
        String fallback = normalize(fallbackName);

        if (target.equalsIgnoreCase(fallback)) {
            throw new IllegalArgumentException("Fallback status must be different");
        }

        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            try {
                if (isProtected(connection, target)) {
                    throw new IllegalArgumentException("Protected status cannot be deleted");
                }
                if (!existsIgnoreCase(connection, fallback)) {
                    throw new IllegalArgumentException("Fallback status does not exist");
                }

                try (PreparedStatement updateTasks = connection.prepareStatement(
                        "UPDATE tasks SET status = ? WHERE lower(status) = lower(?)"
                )) {
                    updateTasks.setString(1, fallback);
                    updateTasks.setString(2, target);
                    updateTasks.executeUpdate();
                }

                try (PreparedStatement deleteStatus = connection.prepareStatement(
                        "DELETE FROM task_statuses WHERE lower(name) = lower(?)"
                )) {
                    deleteStatus.setString(1, target);
                    int count = deleteStatus.executeUpdate();
                    if (count == 0) {
                        throw new IllegalArgumentException("Status not found");
                    }
                }

                connection.commit();
                workspaceStore.reloadStatuses();
                workspaceStore.reloadTasks();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting status", e);
        }
    }

    private boolean existsIgnoreCase(String name) {
        try (Connection connection = database.openConnection()) {
            return existsIgnoreCase(connection, name);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed checking status existence", e);
        }
    }

    private boolean existsIgnoreCase(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM task_statuses WHERE lower(name) = lower(?) LIMIT 1"
        )) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isProtected(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT is_protected FROM task_statuses WHERE lower(name) = lower(?)"
        )) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Status not found");
                }
                return rs.getInt(1) == 1;
            }
        }
    }

    private int nextDisplayOrder(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COALESCE(MAX(display_order), -1) FROM task_statuses")) {
            rs.next();
            return rs.getInt(1) + 1;
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status name is required");
        }
        return value.trim();
    }
}
