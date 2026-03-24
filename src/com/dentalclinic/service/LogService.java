package com.dentalclinic.service;

import com.dentalclinic.dao.LogDAO;
import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogService {
    private LogDAO logDAO = new LogDAO();

    // ==========================================================
    // SECTION 1: USER ACTIVITY LOGS (Human Actions)
    // ==========================================================
    
    // The method to call whenever someone does something important
    public void record(int userId, String role, String action, String details) {
        logDAO.insertLog(userId, role, action, details);
    }

    public List<Object[]> getActivityLogs() throws SQLException {
        return logDAO.getAllLogsWithNames();
    }

    // ==========================================================
    // SECTION 2: SYSTEM LOGS (Technical/Background Events)
    // ==========================================================

    /**
     * STATIC Utility to record system events. 
     * Can be called from anywhere: LogService.logSystemEvent(...)
     */
    public static void logSystemEvent(String level, String source, String message) {
        String sql = "INSERT INTO system_logs (log_level, source_class, message) VALUES (?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, level);   // INFO, WARNING, ERROR
            pstmt.setString(2, source);  // Class name
            pstmt.setString(3, message); // The message
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            // Fallback to console if DB is unreachable
            System.err.println("CRITICAL: Could not write to system_logs table!");
            System.err.println("Log attempted: [" + level + "] " + source + ": " + message);
            e.printStackTrace();
        }
    }

    public List<Object[]> getSystemLogs() throws Exception {
        List<Object[]> logs = new ArrayList<>();
        String query = "SELECT * FROM system_logs ORDER BY timestamp DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(new Object[]{
                    rs.getInt("sys_log_id"),
                    rs.getString("log_level"),
                    rs.getString("source_class"),
                    rs.getString("message"),
                    rs.getTimestamp("timestamp")
                });
            }
        }
        return logs;
    }

    public boolean clearAllSystemLogs() {
        String sql = "DELETE FROM system_logs";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Add this to your LogService.java
    public boolean verifySuperAdminPassword(int adminId, String password) {
        // We check specifically for the ID and if they are a Super Admin
        String sql = "SELECT * FROM staff WHERE staff_id = ? AND password = ? AND is_super_admin = 1";
        try (java.sql.Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, adminId);
            pstmt.setString(2, password);

            java.sql.ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returns true only if ID + Password + SuperAdmin status match
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}