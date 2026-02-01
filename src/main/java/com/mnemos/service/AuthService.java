package com.mnemos.service;

import com.mnemos.util.DatabaseManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    private static final String PASSWORD_KEY = "password_hash";

    /**
     * Hash a password using SHA-256
     */
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Check if a password has been set
     */
    public boolean isPasswordSet() {
        String sql = "SELECT value FROM app_settings WHERE key = ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, PASSWORD_KEY);
            ResultSet rs = pstmt.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Set the password (first time setup)
     */
    public void setPassword(String password) {
        String hash = hashPassword(password);
        String sql = "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, PASSWORD_KEY);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verify a password against the stored hash
     */
    public boolean verifyPassword(String password) {
        String hash = hashPassword(password);
        String sql = "SELECT value FROM app_settings WHERE key = ?";

        try (Connection conn = DatabaseManager.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, PASSWORD_KEY);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("value");
                return hash.equals(storedHash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
