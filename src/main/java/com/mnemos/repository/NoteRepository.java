package com.mnemos.repository;

import com.mnemos.model.Note;
import com.mnemos.util.DatabaseManager;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoteRepository implements GenericRepository<Note, Long> {
    private static final Logger logger = LoggerFactory.getLogger(NoteRepository.class);

    @Override
    public Note save(Note note) {
        String sql;
        boolean isNew = (note.getId() == null);

        if (isNew) {
            sql = "INSERT INTO notes(title, content, created_at, updated_at) VALUES(?, ?, ?, ?)";
        } else {
            sql = "UPDATE notes SET title = ?, content = ?, updated_at = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, note.getTitle());
            pstmt.setString(2, note.getContent());

            if (isNew) {
                long now = Instant.now().toEpochMilli();
                pstmt.setLong(3, now);
                pstmt.setLong(4, now);
                note.setCreatedAt(Instant.ofEpochMilli(now));
                note.setUpdatedAt(Instant.ofEpochMilli(now));
            } else {
                long now = Instant.now().toEpochMilli();
                pstmt.setLong(3, now);
                pstmt.setLong(4, note.getId());
                note.setUpdatedAt(Instant.ofEpochMilli(now));
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating/Updating note failed, no rows affected.");
            }

            if (isNew) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        note.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("Creating note failed, no ID obtained.");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving note", e);
        }
        return note;
    }

    @Override
    public Optional<Note> findById(Long id) {
        String sql = "SELECT * FROM notes WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding note by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Note> findAll() {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT * FROM notes ORDER BY updated_at DESC";

        try (Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all notes", e);
        }
        return notes;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting note", e);
        }
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        return new Note(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("content"),
                Instant.ofEpochMilli(rs.getLong("created_at")),
                Instant.ofEpochMilli(rs.getLong("updated_at")));
    }
}
