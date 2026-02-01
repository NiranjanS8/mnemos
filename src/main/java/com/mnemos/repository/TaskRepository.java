package com.mnemos.repository;

import com.mnemos.model.Task;
import com.mnemos.util.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRepository implements GenericRepository<Task, Long> {
    private static final Logger logger = LoggerFactory.getLogger(TaskRepository.class);

    @Override
    public Task save(Task task) {
        String sql;
        boolean isNew = (task.getId() == null);

        if (isNew) {
            sql = "INSERT INTO tasks(title, priority, due_date, status, completed_at, pomodoro_count, recurrence_type, recurrence_interval, recurrence_end_date, reminder_date) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE tasks SET title = ?, priority = ?, due_date = ?, status = ?, completed_at = ?, pomodoro_count = ?, recurrence_type = ?, recurrence_interval = ?, recurrence_end_date = ?, reminder_date = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getPriority().name());
            pstmt.setString(3, task.getDueDate() != null ? task.getDueDate().toString() : null);
            pstmt.setString(4, task.getStatus().name());

            // Set completed_at timestamp when status is COMPLETED
            if (task.getStatus() == Task.Status.COMPLETED) {
                pstmt.setLong(5, System.currentTimeMillis());
            } else {
                pstmt.setNull(5, Types.INTEGER);
            }

            pstmt.setInt(6, task.getPomodoroCount());

            // New fields
            pstmt.setString(7, task.getRecurrenceType().name());
            pstmt.setInt(8, task.getRecurrenceInterval());
            pstmt.setString(9, task.getRecurrenceEndDate() != null ? task.getRecurrenceEndDate().toString() : null);
            pstmt.setString(10, task.getReminderDate());

            // For UPDATE, add the ID parameter
            if (!isNew) {
                pstmt.setLong(11, task.getId());
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating/Updating task failed, no rows affected.");
            }

            if (isNew) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setId(generatedKeys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving task", e);
        }
        return task;
    }

    @Override
    public Optional<Task> findById(Long id) {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return Optional.of(mapRow(rs));

        } catch (SQLException e) {
            logger.error("Error finding task", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY due_date ASC";
        try (Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                tasks.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error finding all tasks", e);
        }
        return tasks;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting task", e);
        }
    }

    public int deleteOldCompletedTasks(long minutesOld) {
        long cutoffTime = System.currentTimeMillis() - (minutesOld * 60 * 1000);
        String sql = "DELETE FROM tasks WHERE status = 'COMPLETED' AND completed_at IS NOT NULL AND completed_at < ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, cutoffTime);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting old completed tasks", e);
            return 0;
        }
    }

    // Dependency Methods

    public void addDependency(long predecessorId, long successorId) {
        String sql = "INSERT OR IGNORE INTO task_dependencies (predecessor_id, successor_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, predecessorId);
            pstmt.setLong(2, successorId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error adding dependency", e);
        }
    }

    public void removeDependency(long predecessorId, long successorId) {
        String sql = "DELETE FROM task_dependencies WHERE predecessor_id = ? AND successor_id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, predecessorId);
            pstmt.setLong(2, successorId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error removing dependency", e);
        }
    }

    public List<Long> getPredecessors(long taskId) {
        List<Long> ids = new ArrayList<>();
        String sql = "SELECT predecessor_id FROM task_dependencies WHERE successor_id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ids.add(rs.getLong("predecessor_id"));
            }
        } catch (SQLException e) {
            logger.error("Error getting predecessors", e);
        }
        return ids;
    }

    public boolean canStart(long taskId) {
        // A task can start if all its predecessors are COMPLETED
        String sql = """
                    SELECT COUNT(*) FROM task_dependencies td
                    JOIN tasks t ON td.predecessor_id = t.id
                    WHERE td.successor_id = ? AND t.status != 'COMPLETED'
                """;
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) == 0; // 0 blocking tasks means can start
            }
        } catch (SQLException e) {
            logger.error("Error checking canStart", e);
        }
        return true; // Default to true if error
    }

    private Task mapRow(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("due_date");
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : null;

        int pomodoroCount = rs.getInt("pomodoro_count");

        // Map new fields null-safely
        String recTypeStr = rs.getString("recurrence_type");
        Task.RecurrenceType recurrenceType = (recTypeStr != null) ? Task.RecurrenceType.valueOf(recTypeStr)
                : Task.RecurrenceType.NONE;

        int recurrenceInterval = rs.getInt("recurrence_interval"); // 0 if null

        String recEndDateStr = rs.getString("recurrence_end_date");
        LocalDate recurrenceEndDate = (recEndDateStr != null && !recEndDateStr.isEmpty())
                ? LocalDate.parse(recEndDateStr)
                : null;

        String reminderDate = rs.getString("reminder_date");

        Task task = new Task(
                rs.getLong("id"),
                rs.getString("title"),
                Task.Priority.valueOf(rs.getString("priority")),
                date,
                Task.Status.valueOf(rs.getString("status")),
                pomodoroCount);

        task.setRecurrenceType(recurrenceType);
        task.setRecurrenceInterval(recurrenceInterval);
        task.setRecurrenceEndDate(recurrenceEndDate);
        task.setReminderDate(reminderDate);

        return task;
    }
}
