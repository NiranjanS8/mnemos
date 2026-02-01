package com.mnemos.repository;

import com.mnemos.model.FileReference;
import com.mnemos.util.DatabaseManager;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileRepository implements GenericRepository<FileReference, Long> {
    private static final Logger logger = LoggerFactory.getLogger(FileRepository.class);

    @Override
    public FileReference save(FileReference file) {
        String sql = "INSERT INTO files(name, path, type, added_at) VALUES(?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, file.getName());
            pstmt.setString(2, file.getPath());
            pstmt.setString(3, file.getType());
            long now = Instant.now().toEpochMilli();
            pstmt.setLong(4, now);
            file.setAddedAt(Instant.ofEpochMilli(now));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        file.setId(generatedKeys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error saving file reference", e);
        }
        return file;
    }

    @Override
    public Optional<FileReference> findById(Long id) {
        String sql = "SELECT * FROM files WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding file by id", e);
        }
        return Optional.empty();
    }

    @Override
    public List<FileReference> findAll() {
        List<FileReference> files = new ArrayList<>();
        String sql = "SELECT * FROM files ORDER BY added_at DESC";
        try (Connection conn = DatabaseManager.connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                files.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error finding all files", e);
        }
        return files;
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM files WHERE id = ?";
        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting file", e);
        }
    }

    private FileReference mapRow(ResultSet rs) throws SQLException {
        return new FileReference(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("path"),
                rs.getString("type"),
                Instant.ofEpochMilli(rs.getLong("added_at")));
    }
}
