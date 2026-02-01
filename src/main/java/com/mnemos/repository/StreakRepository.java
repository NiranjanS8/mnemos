package com.mnemos.repository;

import com.mnemos.model.Streak;
import com.mnemos.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;

public class StreakRepository {

    public Streak getCurrentStreak() {
        String sql = "SELECT * FROM streaks WHERE id = 1";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Streak(1L, 0, null, 0);
    }

    public void updateStreak(Streak streak) {
        String sql = """
                UPDATE streaks
                SET current_streak = ?,
                    last_completion_date = ?,
                    longest_streak = ?
                WHERE id = 1
                """;

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, streak.getCurrentStreak());
            pstmt.setString(2,
                    streak.getLastCompletionDate() != null ? streak.getLastCompletionDate().toString() : null);
            pstmt.setInt(3, streak.getLongestStreak());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Streak mapRow(ResultSet rs) throws SQLException {
        String dateStr = rs.getString("last_completion_date");
        LocalDate lastDate = dateStr != null ? LocalDate.parse(dateStr) : null;

        return new Streak(
                rs.getLong("id"),
                rs.getInt("current_streak"),
                lastDate,
                rs.getInt("longest_streak"));
    }
}
