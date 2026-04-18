package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportsDAO {

    public List<Object[]> fetchPatientReport(String startDate, String endDate) throws SQLException {
        String query = "SELECT patient_id, first_name, last_name, email, contact_number, registration_date " +
                "FROM patients WHERE registration_date BETWEEN ? AND ? ORDER BY registration_date DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("patient_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("contact_number"),
                            rs.getTimestamp("registration_date")
                    });
                }
            }
        }
        return rows;
    }

    public List<Object[]> fetchAppointmentReport(String startDate, String endDate) throws SQLException {
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                "LEFT JOIN services s ON a.service_id = s.service_id " +
                "WHERE a.appointment_date BETWEEN ? AND ? ORDER BY a.appointment_date DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("appointment_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            rs.getString("service_type"),
                            rs.getDate("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getString("status")
                    });
                }
            }
        }
        return rows;
    }

    public List<Object[]> fetchPendingApprovalsReport(String startDate, String endDate) throws SQLException {
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.request_date " +
                "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                "LEFT JOIN services s ON a.service_id = s.service_id " +
                "WHERE a.status = 'Pending' AND a.appointment_date BETWEEN ? AND ? ORDER BY a.request_date ASC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("appointment_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            rs.getString("service_type"),
                            rs.getDate("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getTimestamp("request_date")
                    });
                }
            }
        }
        return rows;
    }

    public List<Object[]> fetchCompletedTreatmentsReport(String startDate, String endDate) throws SQLException {
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.clinical_notes " +
                "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                "LEFT JOIN services s ON a.service_id = s.service_id " +
                "WHERE a.status = 'Completed' AND a.appointment_date BETWEEN ? AND ? ORDER BY a.appointment_date DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("appointment_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            rs.getString("service_type"),
                            rs.getDate("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getString("clinical_notes") != null ? rs.getString("clinical_notes") : "No notes"
                    });
                }
            }
        }
        return rows;
    }

    public List<Object[]> fetchCancelledAppointmentsReport(String startDate, String endDate) throws SQLException {
        String query = "SELECT a.appointment_id, p.first_name, p.last_name, s.service_name AS service_type, " +
                "a.appointment_date, DATE_FORMAT(a.appointment_time_new, '%h:%i %p') AS appointment_time, a.status " +
                "FROM appointments a JOIN patients p ON a.patient_id = p.patient_id " +
                "LEFT JOIN services s ON a.service_id = s.service_id " +
                "WHERE a.status IN ('Cancelled', 'Declined') AND a.appointment_date BETWEEN ? AND ? ORDER BY a.appointment_date DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("appointment_id"),
                            rs.getString("first_name") + " " + rs.getString("last_name"),
                            rs.getString("service_type"),
                            rs.getDate("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getString("status")
                    });
                }
            }
        }
        return rows;
    }

    public List<Object[]> fetchServicePopularityReport() throws SQLException {
        String query = "SELECT s.service_name AS service_type, COUNT(*) as total, " +
                "SUM(CASE WHEN a.status = 'Completed' THEN 1 ELSE 0 END) as completed, " +
                "SUM(CASE WHEN a.status = 'Approved' THEN 1 ELSE 0 END) as approved, " +
                "SUM(CASE WHEN a.status = 'Pending' THEN 1 ELSE 0 END) as pending, " +
                "SUM(CASE WHEN a.status IN ('Cancelled', 'Declined') THEN 1 ELSE 0 END) as cancelled " +
                "FROM appointments a LEFT JOIN services s ON a.service_id = s.service_id " +
                "GROUP BY s.service_name ORDER BY total DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getString("service_type"),
                        rs.getInt("total"),
                        rs.getInt("completed"),
                        rs.getInt("approved"),
                        rs.getInt("pending"),
                        rs.getInt("cancelled")
                });
            }
        }
        return rows;
    }
}
