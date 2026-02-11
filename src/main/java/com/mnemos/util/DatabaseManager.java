package com.mnemos.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:mnemos.db";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC Driver not found", e);
        }
    }

    public static Connection connect() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Enforce foreign keys
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void initialize() {
        String noteTable = """
                    CREATE TABLE IF NOT EXISTS notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        content TEXT,
                        created_at INTEGER,
                        updated_at INTEGER
                    );
                """;

        String taskTable = """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        priority TEXT,
                        due_date TEXT,
                        status TEXT,
                        completed_at INTEGER,
                        pomodoro_count INTEGER DEFAULT 0
                    );
                """;

        String fileTable = """
                    CREATE TABLE IF NOT EXISTS files (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT,
                        path TEXT,
                        type TEXT,
                        added_at INTEGER
                    );
                """;

        String streakTable = """
                    CREATE TABLE IF NOT EXISTS streaks (
                        id INTEGER PRIMARY KEY,
                        current_streak INTEGER DEFAULT 0,
                        last_completion_date TEXT,
                        longest_streak INTEGER DEFAULT 0
                    );
                """;

        String settingsTable = """
                    CREATE TABLE IF NOT EXISTS app_settings (
                        key TEXT PRIMARY KEY,
                        value TEXT
                    );
                """;

        String linksTable = """
                    CREATE TABLE IF NOT EXISTS links (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        source_type TEXT NOT NULL,
                        source_id INTEGER NOT NULL,
                        target_type TEXT NOT NULL,
                        target_id INTEGER NOT NULL,
                        created_at INTEGER,
                        UNIQUE(source_type, source_id, target_type, target_id)
                    );
                """;

        try (Connection conn = connect();
                var stmt = conn.createStatement()) {

            stmt.execute(noteTable);
            stmt.execute(taskTable);
            stmt.execute(fileTable);
            stmt.execute(streakTable);
            stmt.execute(settingsTable);
            stmt.execute(linksTable);

            // Initialize streak record if not exists
            stmt.execute(
                    "INSERT OR IGNORE INTO streaks (id, current_streak, last_completion_date, longest_streak) VALUES (1, 0, NULL, 0)");

            // Migration: Add completed_at column if it doesn't exist
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN completed_at INTEGER");
                logger.info("Added completed_at column to tasks table");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // Migration: Add pomodoro_count column if it doesn't exist
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN pomodoro_count INTEGER DEFAULT 0");
                logger.info("Added pomodoro_count column to tasks table");
            } catch (SQLException e) {
                // Column already exists, ignore
            }

            // Migration: Add recurrence and reminder columns
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN recurrence_type TEXT");
                stmt.execute("ALTER TABLE tasks ADD COLUMN recurrence_interval INTEGER");
                stmt.execute("ALTER TABLE tasks ADD COLUMN recurrence_end_date TEXT");
                stmt.execute("ALTER TABLE tasks ADD COLUMN reminder_date TEXT");
                logger.info("Added recurrence and reminder columns to tasks table");
            } catch (SQLException e) {
            }

            // Migration: Add custom recurrence columns
            try {
                stmt.execute("ALTER TABLE tasks ADD COLUMN recurrence_unit TEXT");
                stmt.execute("ALTER TABLE tasks ADD COLUMN recurrence_days TEXT");
                stmt.execute("ALTER TABLE tasks ADD COLUMN recurrence_max_occurrences INTEGER DEFAULT 0");
                logger.info("Added custom recurrence columns to tasks table");
            } catch (SQLException e) {
            }

            String taskDependenciesTable = """
                    CREATE TABLE IF NOT EXISTS task_dependencies (
                        predecessor_id INTEGER NOT NULL,
                        successor_id INTEGER NOT NULL,
                        PRIMARY KEY (predecessor_id, successor_id),
                        FOREIGN KEY(predecessor_id) REFERENCES tasks(id) ON DELETE CASCADE,
                        FOREIGN KEY(successor_id) REFERENCES tasks(id) ON DELETE CASCADE
                    );
                    """;
            stmt.execute(taskDependenciesTable);

            logger.info("Database initialized successfully.");

        } catch (SQLException e) {
            logger.error("Database initialization failed", e);
        }
    }
}
