package com.deadlineflow.data.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteMigration {

    public void migrate(SqliteDatabase database) {
        try (Connection connection = database.openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        color TEXT NOT NULL,
                        priority INTEGER NOT NULL CHECK(priority >= 1 AND priority <= 5)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT PRIMARY KEY,
                        project_id INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        start_date TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        progress INTEGER NOT NULL CHECK(progress >= 0 AND progress <= 100),
                        status TEXT NOT NULL,
                        FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS dependencies (
                        id TEXT PRIMARY KEY,
                        from_task_id TEXT NOT NULL,
                        to_task_id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        FOREIGN KEY(from_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                        FOREIGN KEY(to_task_id) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS task_statuses (
                        name TEXT PRIMARY KEY,
                        display_order INTEGER NOT NULL,
                        is_protected INTEGER NOT NULL DEFAULT 0 CHECK(is_protected IN (0, 1))
                    )
                    """);

            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tasks_project_id ON tasks(project_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dependencies_from ON dependencies(from_task_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dependencies_to ON dependencies(to_task_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_statuses_order ON task_statuses(display_order)");

            if (!tableHasColumn(connection, "tasks", "description")) {
                // Migration for existing workspaces: add persisted task description text.
                statement.executeUpdate("ALTER TABLE tasks ADD COLUMN description TEXT NOT NULL DEFAULT ''");
            }

            migrateLegacyStatusValues(connection);
            seedDefaultStatuses(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to migrate SQLite schema", e);
        }
    }

    private boolean tableHasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private void migrateLegacyStatusValues(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE tasks SET status = 'TODO' WHERE status = 'NOT_STARTED'"
        )) {
            statement.executeUpdate();
        }
    }

    private void seedDefaultStatuses(Connection connection) throws SQLException {
        ensureStatus(connection, "TODO", 0, false);
        ensureStatus(connection, "IN_PROGRESS", 1, false);
        ensureStatus(connection, "DONE", 2, true);
        ensureStatus(connection, "BLOCKED", 3, false);
    }

    private void ensureStatus(Connection connection, String name, int order, boolean isProtected) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO task_statuses(name, display_order, is_protected) VALUES (?, ?, ?)"
        )) {
            statement.setString(1, name);
            statement.setInt(2, order);
            statement.setInt(3, isProtected ? 1 : 0);
            statement.executeUpdate();
        }
    }
}
