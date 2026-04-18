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
           ps.setString(2, role);     // This is the data being passed in
           ps.setString(3, action);
           ps.setString(4, details);

           ps.executeUpdate();

       } catch (SQLException e) {
           // This is where your current error is being printed
           System.err.println("Error in insertLog: " + e.getMessage());
           e.printStackTrace();
       }
   }

    public List<Object[]> getAllLogsWithNames() throws SQLException {
        List<Object[]> logs = new ArrayList<>();

        // We join BOTH tables. 
        // p = patients table
        // s = staff table
        String sql = "SELECT l.log_id, l.user_id, l.user_role, l.action, l.details, l.timestamp, " +
                     "p.first_name as p_fname, p.last_name as p_lname, " +
                     "s.full_name as s_fullname " + 
                     "FROM activity_logs l " +
                     "LEFT JOIN patients p ON l.user_id = p.patient_id AND l.user_role = 'Patient' " +
                     "LEFT JOIN staff s ON l.user_id = s.staff_id AND l.user_role != 'Patient' " +
                     "ORDER BY l.timestamp DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String role = rs.getString("user_role");
                String finalName;

                // Logic to determine which name to use
                if ("Patient".equalsIgnoreCase(role)) {
                    String fname = rs.getString("p_fname");
                    String lname = rs.getString("p_lname");
                    finalName = (fname != null) ? (fname + " " + lname) : "Deleted Patient";
                } else {
                    String sName = rs.getString("s_fullname");
                    finalName = (sName != null) ? sName : "System/Unknown";
                }

                logs.add(new Object[]{
                    rs.getInt("log_id"),      // Index 0
                    rs.getInt("user_id"),     // Index 1
                    finalName,                // Index 2 (Now traced correctly!)
                    role,                     // Index 3
                    rs.getString("action"),    // Index 4
                    rs.getString("details"),   // Index 5
                    rs.getTimestamp("timestamp") // Index 6
                });
            }
        }
        return logs;
    }

    public void insertSystemLog(String level, String source, String message) throws SQLException {
        String sql = "INSERT INTO system_logs (log_level, source_class, message) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, level);
            pstmt.setString(2, source);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        }
    }

    public List<Object[]> getSystemLogs() throws SQLException {
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

    public boolean clearAllSystemLogs() throws SQLException {
        String sql = "DELETE FROM system_logs";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;
        }
    }

    public boolean verifySuperAdminPassword(int adminId, String password) throws SQLException {
        String sql = "SELECT password FROM staff WHERE staff_id = ? AND is_super_admin = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                String storedHash = rs.getString("password");
                return com.dentalclinic.util.PasswordUtil.verifyPassword(password, storedHash);
            }
        }
    }

    public boolean archiveActivityLogs() throws SQLException {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;
        }
    }
}
