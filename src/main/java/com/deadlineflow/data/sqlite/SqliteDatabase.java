package com.deadlineflow.data.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteDatabase {
    private final Path dbPath;
    private final String jdbcUrl;

    public SqliteDatabase() {
        this(Paths.get(System.getProperty("user.home"), ".deadlineflow", "workspace.db"));
    }

    public SqliteDatabase(Path dbPath) {
        this.dbPath = dbPath;
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
    }

    public void ensureStorageDirectory() {
        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize workspace directory", e);
        }
    }

    public Connection openConnection() {
        ensureStorageDirectory();
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to open SQLite connection", e);
        }
    }

    public Path dbPath() {
        return dbPath;
    }
}
