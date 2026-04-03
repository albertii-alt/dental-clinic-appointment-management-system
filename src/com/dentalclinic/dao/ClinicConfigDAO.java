package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import com.dentalclinic.service.LogService;
import java.sql.*;

public class ClinicConfigDAO {
    private LogService logService = new LogService();

    // 1. SMART Update Lead Time
    public boolean updateLeadTime(int newDays, int staffId, String role) throws SQLException {
        int currentDays = 0;
        String checkSql = "SELECT setting_value FROM clinic_settings WHERE setting_name = 'min_lead_days'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql);
             ResultSet rs = checkPs.executeQuery()) {
            if (rs.next()) currentDays = rs.getInt("setting_value");
        }

        if (newDays == currentDays) return true; 

        String query = "UPDATE clinic_settings SET setting_value = ? WHERE setting_name = 'min_lead_days'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, newDays);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Update Clinic Setting", 
                    "Changed minimum lead time from " + currentDays + " to " + newDays + " days.");
            }
            return success;
        }
    }

    // 2. SMART Toggle Time Slots
    public boolean updateTimeSlotStatus(String timeSlot, boolean isActive, int staffId, String role) throws SQLException {
        int currentStatus = -1;
        String checkSql = "SELECT is_active FROM clinic_hours WHERE time_slot = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, timeSlot);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) currentStatus = rs.getInt("is_active");
            }
        }

        if (currentStatus == (isActive ? 1 : 0)) return true;

        String query = "UPDATE clinic_hours SET is_active = ? WHERE time_slot = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isActive ? 1 : 0);
            pstmt.setString(2, timeSlot);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Toggle Time Slot", 
                    "Set slot " + timeSlot + " to " + (isActive ? "Active" : "Inactive"));
            }
            return success;
        }
    }

    // 3. SMART Update Day Status
    public boolean updateDayStatus(String dayName, boolean isOpen, int staffId, String role) throws SQLException {
        int currentStatus = -1;
        String checkSql = "SELECT is_open FROM clinic_schedule WHERE day_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, dayName);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) currentStatus = rs.getInt("is_open");
            }
        }

        if (currentStatus == (isOpen ? 1 : 0)) return true;

        String query = "UPDATE clinic_schedule SET is_open = ? WHERE day_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isOpen ? 1 : 0);
            pstmt.setString(2, dayName);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Update Clinic Schedule", 
                    "Set " + dayName + " to " + (isOpen ? "Opened" : "Closed"));
            }
            return success;
        }
    }

    // 4. ADD Time Slot (Missing from your version)
    public boolean addTimeSlot(String newTime, int staffId, String role) throws SQLException {
        String query = "INSERT INTO clinic_hours (time_slot, is_active) VALUES (?, 1) " +
                       "ON DUPLICATE KEY UPDATE is_active = 1"; 
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newTime);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Add Time Slot", "Added new slot: " + newTime);
            }
            return success;
        }
    }

    // 5. DELETE Time Slot (Missing from your version)
    public boolean deleteTimeSlot(String timeSlot, int staffId, String role) throws SQLException {
        String query = "DELETE FROM clinic_hours WHERE time_slot = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, timeSlot);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Delete Time Slot", "Permanently deleted slot: " + timeSlot);
            }
            return success;
        }
    }

    // 6. SMART Update Service Status
    public boolean updateServiceStatus(String serviceName, boolean shouldBeActive, int staffId, String role) throws SQLException {
        int currentStatus = -1;
        String checkSql = "SELECT is_active FROM services WHERE service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, serviceName);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) currentStatus = rs.getInt("is_active");
            }
        }

        if (currentStatus == (shouldBeActive ? 1 : 0)) return true;

        String query = "UPDATE services SET is_active = ? WHERE service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, shouldBeActive ? 1 : 0);
            pstmt.setString(2, serviceName);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Toggle Service", 
                    "Set service " + serviceName + " to " + (shouldBeActive ? "Active" : "Inactive"));
            }
            return success;
        }
    }

    // 7. Delete Service
    public boolean deleteService(String serviceName, int staffId, String role) throws SQLException {
        String query = "DELETE FROM services WHERE service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, serviceName);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Delete Service", "Permanently deleted service: " + serviceName);
            }
            return success;
        }
    }

    // 8. Add Service
    public boolean addService(String name, String desc, double price, int staffId, String role) throws SQLException {
        String query = "INSERT INTO services (service_name, description, price, is_active) VALUES (?, ?, ?, 1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, (desc == null || desc.isEmpty()) ? "No description" : desc);
            pstmt.setDouble(3, price);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Add Service", 
                    "Added " + name + " with price " + price);
            }
            return success;
        }
    }
    
    // 9. Update Service (EDIT)
    public boolean updateService(String oldName, String newName, String description, double price, int staffId, String role) throws SQLException {
        String query = "UPDATE services SET service_name = ?, description = ?, price = ? WHERE service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, (description == null || description.isEmpty()) ? "No description" : description);
            pstmt.setDouble(3, price);
            pstmt.setString(4, oldName);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(staffId, role, "Update Service", 
                    "Updated service: " + oldName + " → " + newName + " (Price: " + price + ")");
            }
            return success;
        }
    }
}