package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.dentalclinic.service.LogService;

public class StaffDAO {
    private LogService logService = new LogService();
    
    public Object[] login(String user, String pass, String role) throws SQLException {
    // Added full_name to the SELECT statement
        String query = "SELECT staff_id, role, is_super_admin, full_name, email FROM staff WHERE username = ? AND password = ? AND role = ? AND is_active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.setString(3, role);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[] { 
                        rs.getInt("staff_id"), 
                        rs.getString("role").toUpperCase(),
                        rs.getBoolean("is_super_admin"),
                        rs.getString("full_name"), // New data at index 3
                        rs.getString("email")
                    }; 
                }
            }
        }
        return null;
    }
    
        // 2. READ: Get All Staff for the Table
    public List<Object[]> getAllStaff() throws SQLException {
        List<Object[]> staffList = new ArrayList<>();
        // Added is_super_admin to the query
        String query = "SELECT staff_id, full_name, username, email, role, is_active, is_super_admin FROM staff ORDER BY staff_id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                staffList.add(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                    rs.getInt("is_super_admin") == 1 // New hidden 7th column (index 6)
                });
            }
        }
        return staffList;
    }
    
    // 1. SMART CREATE: Add New Staff with Log
    public boolean addStaff(String name, String user, String pass, String email, String role, int adminId, String adminRole) throws SQLException {
        String query = "INSERT INTO staff (full_name, username, password, email, role, is_active) VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, user);
            pstmt.setString(3, pass); 
            pstmt.setString(4, email);
            pstmt.setString(5, role);
            
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(adminId, adminRole, "User Management", "Created new " + role + " account for: " + name);
            }
            return success;
        }
    }

    // 2. SMART UPDATE: Compare and Log Changes
    public boolean updateStaff(int targetId, String newName, String newUser, String newEmail, String newRole, String newPass, int adminId, String adminRole) throws SQLException {
        // FETCH OLD DATA FIRST
        String oldName = "", oldRole = "";
        String checkSql = "SELECT full_name, role FROM staff WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, targetId);
            ResultSet rs = checkPs.executeQuery();
            if (rs.next()) {
                oldName = rs.getString("full_name");
                oldRole = rs.getString("role");
            }
        }

        boolean updatePassword = (newPass != null && !newPass.trim().isEmpty());
        String query = updatePassword ? 
            "UPDATE staff SET full_name = ?, username = ?, email = ?, role = ?, password = ? WHERE staff_id = ?" :
            "UPDATE staff SET full_name = ?, username = ?, email = ?, role = ? WHERE staff_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newUser);
            pstmt.setString(3, newEmail);
            pstmt.setString(4, newRole);
            if (updatePassword) {
                pstmt.setString(5, newPass);
                pstmt.setInt(6, targetId);
            } else {
                pstmt.setInt(5, targetId);
            }

            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                String details = "Updated profile for " + oldName;
                if (!oldRole.equals(newRole)) details += " (Role changed: " + oldRole + " -> " + newRole + ")";
                if (updatePassword) details += " [Password Reset]";
                
                logService.record(adminId, adminRole, "User Management", details);
            }
            return success;
        }
    }

    // 3. SMART TOGGLE: Log Activation/Deactivation
    public boolean toggleStaffStatus(int targetId, boolean isCurrentlyActive, int adminId, String adminRole) throws SQLException {
        String targetName = "";
        // Get name for the log
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT full_name FROM staff WHERE staff_id = ?")) {
            ps.setInt(1, targetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) targetName = rs.getString("full_name");
        }

        String query = "UPDATE staff SET is_active = ? WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, isCurrentlyActive ? 0 : 1);
            pstmt.setInt(2, targetId);
            
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                String action = isCurrentlyActive ? "DEACTIVATED" : "ACTIVATED";
                logService.record(adminId, adminRole, "User Management", action + " account for: " + targetName);
            }
            return success;
        }
    }

    // 4. SMART DELETE: Log Permanent Removal
    public boolean deleteStaff(int targetId, String targetName, int adminId, String adminRole) throws SQLException {
        String query = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, targetId);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(adminId, adminRole, "User Management", "Permanently DELETED user: " + targetName);
            }
            return success;
        }
    }
    
    // ==========================================================
    // SECTION: ACCOUNT SETTINGS (SELF-MANAGEMENT)
    // ==========================================================

    /**
     * Checks if the current admin's password is correct before allowing changes.
     */
    public boolean verifyPassword(int adminId, String password) throws SQLException {
        String sql = "SELECT staff_id FROM staff WHERE staff_id = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Returns true if credentials match
            }
        }
    }

    public boolean updateSelf(int adminId, String newName, String newUser, String newEmail, String newPass, String adminRole) throws SQLException {
        boolean updatePassword = (newPass != null && !newPass.trim().isEmpty());

        // Added username = ? to the SQL queries
        String query = updatePassword ? 
            "UPDATE staff SET full_name = ?, username = ?, email = ?, password = ? WHERE staff_id = ?" :
            "UPDATE staff SET full_name = ?, username = ?, email = ? WHERE staff_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newName);
            pstmt.setString(2, newUser); // New parameter for Username
            pstmt.setString(3, newEmail);

            if (updatePassword) {
                pstmt.setString(4, newPass);
                pstmt.setInt(5, adminId);
            } else {
                pstmt.setInt(4, adminId);
            }

            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                String details = "Admin updated their own profile settings (Name/User/Email).";
                if (updatePassword) details += " [Password changed]";
                logService.record(adminId, adminRole, "Account Settings", details);
            }
            return success;
        }
    }
    
}