package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;

public class ClinicConfigDAO {

    // 1. Update Lead Time (Days before a patient can book)
    public boolean updateLeadTime(int days, int staffId, String role) throws SQLException {
        String query = "UPDATE clinic_settings SET setting_value = ? WHERE setting_name = 'min_lead_days'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, days);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                new com.dentalclinic.service.LogService().record(staffId, role, "Update Clinic Setting", 
                    "Service: Configuration | Details: Changed minimum lead time to " + days + " days.");
            }
            return success;
        }
    }

    // 2. Toggle Time Slots (Active/Inactive)
    public boolean updateTimeSlotStatus(String timeSlot, boolean isActive) throws SQLException {
        String query = "UPDATE clinic_hours SET is_active = ? WHERE time_slot = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isActive ? 1 : 0);
            pstmt.setString(2, timeSlot);
            return pstmt.executeUpdate() > 0;
        }
    }

    // 3. Toggle Clinic Open/Closed Days (e.g., set Sunday to 0)
    public boolean updateDayStatus(String dayName, boolean isOpen, int staffId, String role) throws SQLException {
        String query = "UPDATE clinic_schedule SET is_open = ? WHERE day_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isOpen ? 1 : 0);
            pstmt.setString(2, dayName);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                String status = isOpen ? "Opened" : "Closed";
                new com.dentalclinic.service.LogService().record(staffId, role, "Update Clinic Schedule", 
                    "Service: Schedule | Details: Set " + dayName + " to " + status);
            }
            return success;
        }
    }
    // Add this to com.dentalclinic.dao.ClinicConfigDAO
    public boolean addTimeSlot(String newTime) throws SQLException {
        // We insert it as 'active' (1) by default
        String query = "INSERT INTO clinic_hours (time_slot, is_active) VALUES (?, 1) " +
                       "ON DUPLICATE KEY UPDATE is_active = 1"; 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newTime);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean deleteTimeSlot(String timeSlot) throws SQLException {
        String query = "DELETE FROM clinic_hours WHERE time_slot = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, timeSlot);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // Add to ClinicConfigDAO.java
    public boolean addService(String name, String desc, double price, int staffId, String role) throws SQLException {
        String query = "INSERT INTO services (service_name, description, price, is_active) VALUES (?, ?, ?, 1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, (desc == null || desc.isEmpty()) ? "No description" : desc);
            pstmt.setDouble(3, price);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                new com.dentalclinic.service.LogService().record(staffId, role, "Add Service", 
                    "Service: " + name + " | Details: Added new service with price " + price);
            }
            return success;
        }
    }

    public boolean deleteService(String serviceName) throws SQLException {
        String query = "DELETE FROM services WHERE service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, serviceName);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean toggleServiceStatus(String serviceName, boolean currentStatus) throws SQLException {
        // If currentStatus is true (1), set to 0. If false (0), set to 1.
        String query = "UPDATE services SET is_active = ? WHERE service_name = ?";
        try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentStatus ? 0 : 1);
            pstmt.setString(2, serviceName);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean updateServiceStatus(String serviceName, boolean shouldBeActive) throws SQLException {
        String query = "UPDATE services SET is_active = ? WHERE service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, shouldBeActive ? 1 : 0);
            pstmt.setString(2, serviceName);
            return pstmt.executeUpdate() > 0;
        }
    }
    
}