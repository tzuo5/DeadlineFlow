package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.WorkspaceRepository;
import com.deadlineflow.domain.model.Dependency;
import com.deadlineflow.domain.model.DependencyType;
import com.deadlineflow.domain.model.Project;
import com.deadlineflow.domain.model.StatusDefinition;
import com.deadlineflow.domain.model.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SqliteWorkspaceStore implements WorkspaceRepository {
    private final SqliteDatabase database;
    private final SqliteMigration migration;
    private final SampleDataSeeder sampleDataSeeder;

    private final ObservableList<Project> projects = FXCollections.observableArrayList();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<Dependency> dependencies = FXCollections.observableArrayList();
    private final ObservableList<StatusDefinition> statuses = FXCollections.observableArrayList();

    public SqliteWorkspaceStore(SqliteDatabase database, SqliteMigration migration, SampleDataSeeder sampleDataSeeder) {
        this.database = database;
        this.migration = migration;
        this.sampleDataSeeder = sampleDataSeeder;
    }

    @Override
    public void initialize() {
        migration.migrate(database);
        sampleDataSeeder.seedIfEmpty(database);
        reload();
    }

    @Override
    public void reload() {
        projects.setAll(loadProjects());
        tasks.setAll(loadTasks());
        dependencies.setAll(loadDependencies());
        statuses.setAll(loadStatuses());
    }

    public void reloadProjects() {
        projects.setAll(loadProjects());
    }

    public void reloadTasks() {
        tasks.setAll(loadTasks());
    }

    public void reloadDependencies() {
        dependencies.setAll(loadDependencies());
    }

    public void reloadStatuses() {
        statuses.setAll(loadStatuses());
    }

    public ObservableList<Project> projects() {
        return projects;
    }

    public ObservableList<Task> tasks() {
        return tasks;
    }

    public ObservableList<Dependency> dependencies() {
        return dependencies;
    }

    public ObservableList<StatusDefinition> statuses() {
        return statuses;
    }

    private ObservableList<Project> loadProjects() {
        ObservableList<Project> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, name, color, priority FROM projects ORDER BY priority ASC, name ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Project(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("color"),
                        rs.getInt("priority")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading projects", e);
        }
        return result;
    }

    private ObservableList<Task> loadTasks() {
        ObservableList<Task> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, project_id, title, description, start_date, due_date, progress, status FROM tasks ORDER BY start_date ASC, due_date ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Task(
                        rs.getString("id"),
                        rs.getLong("project_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        java.time.LocalDate.parse(rs.getString("start_date")),
                        java.time.LocalDate.parse(rs.getString("due_date")),
                        rs.getInt("progress"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading tasks", e);
        }
        return result;
    }

    private ObservableList<Dependency> loadDependencies() {
        ObservableList<Dependency> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, from_task_id, to_task_id, type FROM dependencies ORDER BY id ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new Dependency(
                        rs.getString("id"),
                        rs.getString("from_task_id"),
                        rs.getString("to_task_id"),
                        DependencyType.valueOf(rs.getString("type"))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading dependencies", e);
        }
        return result;
    }

    private ObservableList<StatusDefinition> loadStatuses() {
        ObservableList<StatusDefinition> result = FXCollections.observableArrayList();
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT name, is_protected FROM task_statuses ORDER BY display_order ASC, name ASC"
             );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new StatusDefinition(
                        rs.getString("name"),
                        rs.getInt("is_protected") == 1
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed loading statuses", e);
        }
        return result;
    }
}
