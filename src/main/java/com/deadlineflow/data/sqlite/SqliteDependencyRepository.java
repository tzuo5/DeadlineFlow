package com.deadlineflow.data.sqlite;

import com.deadlineflow.data.repository.DependencyRepository;
import com.deadlineflow.domain.model.Dependency;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class SqliteDependencyRepository implements DependencyRepository {
    private final SqliteDatabase database;
    private final SqliteWorkspaceStore workspaceStore;

    public SqliteDependencyRepository(SqliteDatabase database, SqliteWorkspaceStore workspaceStore) {
        this.database = database;
        this.workspaceStore = workspaceStore;
    }

    @Override
    public ObservableList<Dependency> getAll() {
        return workspaceStore.dependencies();
    }

    @Override
    public Optional<Dependency> findById(String dependencyId) {
        return workspaceStore.dependencies().stream()
                .filter(dependency -> dependency.id().equals(dependencyId))
                .findFirst();
    }

    @Override
    public Dependency save(Dependency dependency) {
        if (findById(dependency.id()).isPresent()) {
            return update(dependency);
        }
        return insert(dependency);
    }

    @Override
    public void delete(String dependencyId) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM dependencies WHERE id = ?")) {
            statement.setString(1, dependencyId);
            statement.executeUpdate();
            workspaceStore.reloadDependencies();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed deleting dependency", e);
        }
    }

    private Dependency insert(Dependency dependency) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO dependencies(id, from_task_id, to_task_id, type) VALUES (?, ?, ?, ?)"
             )) {
            statement.setString(1, dependency.id());
            statement.setString(2, dependency.fromTaskId());
            statement.setString(3, dependency.toTaskId());
            statement.setString(4, dependency.type().name());
            statement.executeUpdate();
            workspaceStore.reloadDependencies();
            return dependency;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed creating dependency", e);
        }
    }

    private Dependency update(Dependency dependency) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dependencies SET from_task_id = ?, to_task_id = ?, type = ? WHERE id = ?"
             )) {
            statement.setString(1, dependency.fromTaskId());
            statement.setString(2, dependency.toTaskId());
            statement.setString(3, dependency.type().name());
            statement.setString(4, dependency.id());
            statement.executeUpdate();
            workspaceStore.reloadDependencies();
            return dependency;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed updating dependency", e);
        }
    }
}
