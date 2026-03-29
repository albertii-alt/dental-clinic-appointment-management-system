package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class DashboardDAO {

    public Map<String, Integer> getQuickStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String query = 
            "SELECT " +
            "  (SELECT COUNT(*) FROM patients) AS total_patients, " +
            "  (SELECT COUNT(*) FROM appointments WHERE status = 'Pending') AS pending_appointments, " +
            "  (SELECT COUNT(*) FROM appointments WHERE appointment_date = CURDATE() AND status = 'Approved') AS today_appointments, " +
            "  (SELECT COUNT(*) FROM staff WHERE is_active = 1) AS active_staff";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                stats.put("totalPatients", rs.getInt("total_patients"));
                stats.put("pendingAppts", rs.getInt("pending_appointments"));
                stats.put("todayAppts", rs.getInt("today_appointments"));
                stats.put("activeStaff", rs.getInt("active_staff"));
            }
        }
        return stats;
    }

    public List<Object[]> getTodayAppointments() throws SQLException {
        List<Object[]> appts = new ArrayList<>();
        String query = "SELECT CONCAT(p.first_name, ' ', p.last_name) AS patient_name, " +
                       "a.service_type, a.appointment_time, a.status " +
                       "FROM appointments a " +
                       "JOIN patients p ON a.patient_id = p.patient_id " +
                       "WHERE a.appointment_date = CURDATE() " +
                       "ORDER BY a.appointment_time ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                appts.add(new Object[]{
                    rs.getString("patient_name"),
                    rs.getString("service_type"),
                    rs.getString("appointment_time"),
                    rs.getString("status")
                });
            }
        }
        return appts;
    }

    public List<String[]> getRecentActivityWithDetails() throws SQLException {
        List<String[]> activities = new ArrayList<>();
        String query = "SELECT action, details, timestamp FROM activity_logs ORDER BY timestamp DESC LIMIT 5";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                java.sql.Timestamp ts = rs.getTimestamp("timestamp");
                String time = new java.text.SimpleDateFormat("HH:mm").format(ts);
                activities.add(new String[]{
                    "[" + time + "] " + rs.getString("action"), 
                    rs.getString("details")
                });
            }
        }
        return activities;
    }

    public Map<String, Integer> getServicePopularity() throws SQLException {
        Map<String, Integer> data = new HashMap<>();
        String query = "SELECT service_type, COUNT(*) as count FROM appointments GROUP BY service_type LIMIT 5";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("service_type"), rs.getInt("count"));
            }
        }
        return data;
    }
    
    public Map<String, Integer> getAppointmentTrends() throws SQLException {
        Map<String, Integer> trends = new java.util.LinkedHashMap<>();
        String query = "SELECT service_type, COUNT(*) as total FROM appointments WHERE status != 'Cancelled' GROUP BY service_type";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                trends.put(rs.getString("service_type"), rs.getInt("total"));
            }
        }
        return trends;
    }
}