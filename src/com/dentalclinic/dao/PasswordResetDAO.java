package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class PasswordResetDAO {
    
    private static final int CODE_EXPIRY_MINUTES = 15;
    private static final int MAX_REQUESTS_PER_HOUR = 3;
    
    /**
     * Generate a random 6-digit code
     */
    public static String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
    
    /**
     * Get email by username (checks both patients and staff tables)
     */
    public String getEmailByUsername(String username) throws SQLException {
        // Check in patients table
        String patientQuery = "SELECT email FROM patients WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(patientQuery)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        
        // Check in staff table
        String staffQuery = "SELECT email FROM staff WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(staffQuery)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get user type by username
     */
    public String getUserTypeByUsername(String username) throws SQLException {
        // Check in patients table
        String patientQuery = "SELECT 'patient' as user_type FROM patients WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(patientQuery)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return "patient";
                }
            }
        }
        
        // Check in staff table
        String staffQuery = "SELECT 'staff' as user_type FROM staff WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(staffQuery)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return "staff";
                }
            }
        }
        
        return null;
    }
    
    /**
     * Save reset code to database
     */
    public boolean saveResetCode(String email, String code, String userType, String username) throws SQLException {
        // First, check rate limit
        if (isRateLimited(email)) {
            return false;
        }
        
        // Delete any existing unused codes for this email
        deleteExistingCodes(email);
        
        // Calculate expiry time (15 minutes from now)
        Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES));
        
        String query = "INSERT INTO password_reset_codes (email, code, user_type, expires_at, username) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, code);
            pstmt.setString(3, userType);
            pstmt.setTimestamp(4, expiresAt);
            pstmt.setString(5, username);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Check if email is rate limited (max 3 requests per hour)
     */
    private boolean isRateLimited(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM password_reset_codes WHERE email = ? AND created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= MAX_REQUESTS_PER_HOUR;
                }
            }
        }
        return false;
    }
    
    /**
     * Delete existing unused codes for an email
     */
    private void deleteExistingCodes(String email) throws SQLException {
        String query = "DELETE FROM password_reset_codes WHERE email = ? AND used = 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Verify reset code
     * @return user email if valid, null otherwise
     */
    public String verifyCode(String code) throws SQLException {
        String query = "SELECT email, expires_at, used FROM password_reset_codes WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getInt("used") == 1) {
                        return null;
                    }
                    
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (expiresAt.before(Timestamp.valueOf(LocalDateTime.now()))) {
                        return null;
                    }
                    
                    return rs.getString("email");
                }
            }
        }
        return null;
    }
    
    /**
     * Mark code as used (after successful password reset)
     */
    public void markCodeAsUsed(String code) throws SQLException {
        String query = "UPDATE password_reset_codes SET used = 1 WHERE code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Update password for patient by username
     */
    public boolean updatePatientPasswordByUsername(String username, String hashedPassword) throws SQLException {
        String query = "UPDATE patients SET password = ?, force_password_reset = 0 WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Update password for staff by username
     */
    public boolean updateStaffPasswordByUsername(String username, String hashedPassword) throws SQLException {
        String query = "UPDATE staff SET password = ?, force_password_reset = 0 WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        }
    }
}