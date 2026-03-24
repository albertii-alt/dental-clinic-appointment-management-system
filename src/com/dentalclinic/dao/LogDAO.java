package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    public void insertLog(int userId, String role, String action, String details) {
        String query = "INSERT INTO activity_logs (user_id, user_role, action, details) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            ps.setString(2, role);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Object[]> getAllLogsWithNames() throws SQLException {
    List<Object[]> logs = new ArrayList<>();
    
    // THE ROBUST FIX:
    // 1. We use UPPER(l.user_role) to handle any casing (Patient vs PATIENT).
    // 2. We use LEFT JOIN so we don't 'drop' logs that don't have a staff_id match.
    // 3. We use COALESCE to provide a fallback name if both joins somehow fail.
    String query = "SELECT l.log_id, " +
                   "CASE " +
                   "  WHEN UPPER(l.user_role) = 'PATIENT' THEN CONCAT(p.first_name, ' ', p.last_name) " +
                   "  ELSE s.full_name " +
                   "END as actor_name, " +
                   "l.user_role, l.action, l.details, l.timestamp " +
                   "FROM activity_logs l " +
                   "LEFT JOIN staff s ON l.user_id = s.staff_id AND UPPER(l.user_role) != 'PATIENT' " +
                   "LEFT JOIN patients p ON l.user_id = p.patient_id AND UPPER(l.user_role) = 'PATIENT' " +
                   "ORDER BY l.timestamp DESC";
    
    try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {
        
        while (rs.next()) {
            logs.add(new Object[]{
                rs.getInt("log_id"),
                // If actor_name is null (user deleted), show 'Unknown User'
                rs.getString("actor_name") != null ? rs.getString("actor_name") : "Unknown User",
                rs.getString("user_role"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getTimestamp("timestamp")
            });
        }
    }
    return logs;
}
}