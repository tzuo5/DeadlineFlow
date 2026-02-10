package com.deadlineflow.data.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class SampleDataSeeder {

    public void seedIfEmpty(SqliteDatabase database) {
        try (Connection connection = database.openConnection()) {
            if (!isEmpty(connection)) {
                return;
            }

            long projectA = insertProject(connection, "Launch Alpha", "#2B6EF2", 1);
            long projectB = insertProject(connection, "Client Revamp", "#1AA179", 2);

            LocalDate today = LocalDate.now();

            String t1 = "11111111-1111-4111-8111-111111111111";
            String t2 = "22222222-2222-4222-8222-222222222222";
            String t3 = "33333333-3333-4333-8333-333333333333";
            String t4 = "44444444-4444-4444-8444-444444444444";

            String t5 = "55555555-5555-4555-8555-555555555555";
            String t6 = "66666666-6666-4666-8666-666666666666";
            String t7 = "77777777-7777-4777-8777-777777777777";
            String t8 = "88888888-8888-4888-8888-888888888888";

            insertTask(connection, t1, projectA, "Define scope", "Scope definition", today.minusDays(8), today.minusDays(5), 100, "DONE");
            insertTask(connection, t2, projectA, "Build prototype", "Implementation details", today.minusDays(4), today.plusDays(1), 60, "IN_PROGRESS");
            insertTask(connection, t3, projectA, "QA signoff", "Testing notes", today.plusDays(1), today.plusDays(4), 0, "TODO");
            insertTask(connection, t4, projectA, "Launch prep", "Release checklist", today.plusDays(5), today.plusDays(7), 0, "BLOCKED");

            insertTask(connection, t5, projectB, "Kickoff", "No description", today.minusDays(6), today.minusDays(4), 100, "DONE");
            insertTask(connection, t6, projectB, "Design system", "Scope definition", today.minusDays(3), today.plusDays(2), 35, "IN_PROGRESS");
            insertTask(connection, t7, projectB, "Implement screens", "Implementation details", today.plusDays(3), today.plusDays(8), 0, "TODO");
            insertTask(connection, t8, projectB, "Client review", "Release checklist", today.plusDays(9), today.plusDays(10), 0, "BLOCKED");

            insertDependency(connection, "a1111111-1111-4111-8111-111111111111", t1, t2);
            insertDependency(connection, "a2222222-2222-4222-8222-222222222222", t2, t3); // intentionally violated on seed data
            insertDependency(connection, "a3333333-3333-4333-8333-333333333333", t3, t4);

            insertDependency(connection, "b1111111-1111-4111-8111-111111111111", t5, t6);
            insertDependency(connection, "b2222222-2222-4222-8222-222222222222", t6, t7);
            insertDependency(connection, "b3333333-3333-4333-8333-333333333333", t7, t8);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed seeding sample workspace data", e);
        }
    }

    private boolean isEmpty(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM projects")) {
            return resultSet.next() && resultSet.getInt(1) == 0;
        }
    }

    private long insertProject(Connection connection, String name, String color, int priority) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO projects(name, color, priority) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, name);
            statement.setString(2, color);
            statement.setInt(3, priority);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert sample project " + name);
    }

    private void insertTask(
            Connection connection,
            String id,
            long projectId,
            String title,
            String description,
            LocalDate start,
            LocalDate due,
            int progress,
            String status
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tasks(id, project_id, title, description, start_date, due_date, progress, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        )) {
            statement.setString(1, id);
            statement.setLong(2, projectId);
            statement.setString(3, title);
            statement.setString(4, description);
            statement.setString(5, start.toString());
            statement.setString(6, due.toString());
            statement.setInt(7, progress);
            statement.setString(8, status);
            statement.executeUpdate();
        }
    }

    private void insertDependency(Connection connection, String id, String fromTaskId, String toTaskId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO dependencies(id, from_task_id, to_task_id, type) VALUES (?, ?, ?, ?)"
        )) {
            statement.setString(1, id);
            statement.setString(2, fromTaskId);
            statement.setString(3, toTaskId);
            statement.setString(4, "FINISH_START");
            statement.executeUpdate();
        }
    }
}
