package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.ProjectRepository;
import com.deadlineflow.domain.model.Project;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SqliteProjectRepository implements ProjectRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteProjectRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<Project> getAll() {
        return workspaceStore.projects();
    }

    @Override
    public Optional<Project> findById(long id) {
        return workspaceStore.projects().stream().filter(p -> p.id() == id).findFirst();
    }

    @Override
    public Project save(Project project) {
        if (project.id() <= 0) {
            return insert(project);
        }
        return update(project);
    }

    @Override
    public void delete(long projectId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM projects WHERE id = ?")) {
            statement.setLong(1, projectId);
            statement.executeUpdate();
            workspaceStore.reloadProjects();
            workspaceStore.reloadTasks();
            workspaceStore.reloadDependencies();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting project", e);
        }
    }

    private Project insert(Project project) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO projects(name, color, priority) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, project.name());
            statement.setString(2, project.color());
            statement.setInt(3, project.priority());
            statement.executeUpdate();

            long generatedId;
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("No generated key for inserted project");
                }
                generatedId = generatedKeys.getLong(1);
            }

            workspaceStore.reloadProjects();
            return project.withId(generatedId);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed creating project", e);
        }
    }

    private Project update(Project project) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE projects SET name = ?, color = ?, priority = ? WHERE id = ?"
             )) {
            statement.setString(1, project.name());
            statement.setString(2, project.color());
            statement.setInt(3, project.priority());
            statement.setLong(4, project.id());
            statement.executeUpdate();
            workspaceStore.reloadProjects();
            return project;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed updating project", e);
        }
    }
}
