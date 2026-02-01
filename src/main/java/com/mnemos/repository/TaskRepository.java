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
            sql = "INSERT INTO tasks(title, priority, due_date, status, completed_at, pomodoro_count) VALUES(?, ?, ?, ?, ?, ?)";
        } else {
            sql = "UPDATE tasks SET title = ?, priority = ?, due_date = ?, status = ?, completed_at = ?, pomodoro_count = ? WHERE id = ?";
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

            // For UPDATE, add the ID parameter
            if (!isNew) {
                pstmt.setLong(7, task.getId());
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

    private Task mapRow(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("due_date");
        LocalDate date = (dateStr != null && !dateStr.isEmpty()) ? LocalDate.parse(dateStr) : null;

        int pomodoroCount = rs.getInt("pomodoro_count");

        return new Task(
                rs.getLong("id"),
                rs.getString("title"),
                Task.Priority.valueOf(rs.getString("priority")),
                date,
                Task.Status.valueOf(rs.getString("status")),
                pomodoroCount);
    }
}
