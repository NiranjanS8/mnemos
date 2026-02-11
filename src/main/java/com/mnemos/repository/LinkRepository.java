package com.mnemos.repository;

import com.mnemos.model.Link;
import com.mnemos.model.Link.ItemType;
import com.mnemos.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LinkRepository {

    public Link save(Link link) {
        String sql = """
                INSERT OR IGNORE INTO links (source_type, source_id, target_type, target_id, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, link.getSourceType().name());
            pstmt.setLong(2, link.getSourceId());
            pstmt.setString(3, link.getTargetType().name());
            pstmt.setLong(4, link.getTargetId());
            pstmt.setLong(5, link.getCreatedAt() != null ? link.getCreatedAt() : System.currentTimeMillis());

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                link.setId(rs.getLong(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return link;
    }

    public void delete(Long linkId) {
        String sql = "DELETE FROM links WHERE id = ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, linkId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Link> getLinksForItem(ItemType type, Long id) {
        List<Link> links = new ArrayList<>();
        String sql = """
                SELECT * FROM links
                WHERE (source_type = ? AND source_id = ?)
                   OR (target_type = ? AND target_id = ?)
                """;

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, type.name());
            pstmt.setLong(2, id);
            pstmt.setString(3, type.name());
            pstmt.setLong(4, id);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                links.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return links;
    }

    public boolean isLinked(ItemType sourceType, Long sourceId, ItemType targetType, Long targetId) {
        String sql = """
                SELECT COUNT(*) FROM links
                WHERE (source_type = ? AND source_id = ? AND target_type = ? AND target_id = ?)
                   OR (source_type = ? AND source_id = ? AND target_type = ? AND target_id = ?)
                """;

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sourceType.name());
            pstmt.setLong(2, sourceId);
            pstmt.setString(3, targetType.name());
            pstmt.setLong(4, targetId);
            pstmt.setString(5, targetType.name());
            pstmt.setLong(6, targetId);
            pstmt.setString(7, sourceType.name());
            pstmt.setLong(8, sourceId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Link mapRow(ResultSet rs) throws SQLException {
        Link link = new Link();
        link.setId(rs.getLong("id"));
        link.setSourceType(ItemType.valueOf(rs.getString("source_type")));
        link.setSourceId(rs.getLong("source_id"));
        link.setTargetType(ItemType.valueOf(rs.getString("target_type")));
        link.setTargetId(rs.getLong("target_id"));
        link.setCreatedAt(rs.getLong("created_at"));
        return link;
    }
}
