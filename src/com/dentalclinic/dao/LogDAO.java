package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {

    public void insertLog(int userId, String role, String action, String details) {
       // FIXED: Changed 'role' to 'user_role' to match your database schema in hehe.png
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

        // FIXED: Changed l.role to l.user_role to match your 'hehe.png' screenshot
        String sql = "SELECT l.log_id, l.user_id, u.full_name, l.user_role, l.action, l.details, l.timestamp " +
                     "FROM activity_logs l " +
                     "LEFT JOIN staff u ON l.user_id = u.staff_id " + 
                     "ORDER BY l.timestamp DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(new Object[]{
                    rs.getInt("log_id"),      // Index 0
                    rs.getInt("user_id"),     // Index 1
                    rs.getString("full_name") != null ? rs.getString("full_name") : "System/Unknown", // Index 2
                    rs.getString("user_role"), // Index 3 (Changed from "role" to "user_role")
                    rs.getString("action"),    // Index 4
                    rs.getString("details"),   // Index 5
                    rs.getTimestamp("timestamp") // Index 6
                });
            }
        }
        return logs;
    }
}