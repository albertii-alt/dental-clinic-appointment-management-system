package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import com.dentalclinic.util.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.dentalclinic.service.LogService;
import java.sql.Timestamp;

public class StaffDAO {
    private LogService logService = new LogService();
    
    // ==========================================================
    // AUTHENTICATION
    // ==========================================================
    
    public Object[] login(String user, String pass, String role) throws SQLException {
        // SECURITY FIX: First get user by username and role only, then verify password hash
        String query = "SELECT s.staff_id, r.role_name AS role, s.is_super_admin, s.full_name, s.email, s.password, s.force_password_reset " +
                       "FROM staff s JOIN roles r ON s.role_id = r.role_id " +
                       "WHERE s.username = ? AND r.role_name = ? AND s.is_active = 1";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, user);
            pstmt.setString(2, role);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    
                    // SECURITY FIX: Verify password against hash
                    if (PasswordUtil.verifyPassword(pass, storedHash)) {
                        return new Object[] { 
                            rs.getInt("staff_id"), 
                            rs.getString("role").toUpperCase(),
                            rs.getBoolean("is_super_admin"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getInt("force_password_reset") // Include reset flag
                        };
                    }
                }
            }
        }
        return null;
    }
    
    // ==========================================================
    // PASSWORD RESET FLAG MANAGEMENT
    // ==========================================================
    
    public boolean needsPasswordReset(int staffId) throws SQLException {
        String query = "SELECT force_password_reset FROM staff WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, staffId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("force_password_reset") == 1;
                }
            }
        }
        return false;
    }
    
    public void clearPasswordResetFlag(int staffId) throws SQLException {
        String query = "UPDATE staff SET force_password_reset = 0 WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, staffId);
            pstmt.executeUpdate();
        }
    }

    public boolean updatePasswordAndClearReset(int staffId, String hashedPassword) throws SQLException {
        String query = "UPDATE staff SET password = ?, force_password_reset = 0 WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, staffId);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // ==========================================================
    // STAFF CRUD OPERATIONS
    // ==========================================================
    
    public List<Object[]> getAllStaff() throws SQLException {
        List<Object[]> staffList = new ArrayList<>();
        String query = "SELECT s.staff_id, s.full_name, s.username, s.email, r.role_name AS role, s.is_active, s.is_super_admin " +
                       "FROM staff s JOIN roles r ON s.role_id = r.role_id ORDER BY s.staff_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                staffList.add(new Object[]{
                    rs.getInt("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getInt("is_active") == 1 ? "Active" : "Inactive",
                    rs.getInt("is_super_admin") == 1
                });
            }
        }
        return staffList;
    }
    
    public boolean addStaff(String name, String user, String pass, String email, String role, int adminId, String adminRole) throws SQLException {
        String query = "INSERT INTO staff (full_name, username, password, email, role_id, is_active, force_password_reset) " +
                       "VALUES (?, ?, ?, ?, (SELECT role_id FROM roles WHERE role_name = ?), 1, 0)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, user);
            // SECURITY FIX: Hash the password before storing
            pstmt.setString(3, PasswordUtil.hashPassword(pass));
            pstmt.setString(4, email);
            pstmt.setString(5, role);
            
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                logService.record(adminId, adminRole, "User Management", "Created new " + role + " account for: " + name);
            }
            return success;
        }
    }
    
    public boolean updateStaff(int targetId, String newName, String newUser, String newEmail, String newRole, String newPass, int adminId, String adminRole) throws SQLException {
        // FETCH OLD DATA FIRST
        String oldName = "", oldRole = "";
        String checkSql = "SELECT s.full_name, r.role_name AS role " +
                         "FROM staff s JOIN roles r ON s.role_id = r.role_id WHERE s.staff_id = ?";
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
            "UPDATE staff SET full_name = ?, username = ?, email = ?, role_id = (SELECT role_id FROM roles WHERE role_name = ?), password = ? WHERE staff_id = ?" :
            "UPDATE staff SET full_name = ?, username = ?, email = ?, role_id = (SELECT role_id FROM roles WHERE role_name = ?) WHERE staff_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newUser);
            pstmt.setString(3, newEmail);
            pstmt.setString(4, newRole);
            if (updatePassword) {
                // SECURITY FIX: Hash the new password
                pstmt.setString(5, PasswordUtil.hashPassword(newPass));
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
    // ACCOUNT SETTINGS (SELF-MANAGEMENT)
    // ==========================================================
    
    public boolean verifyPassword(int adminId, String password) throws SQLException {
        // SECURITY FIX: Get hash and verify, instead of direct comparison
        String sql = "SELECT password FROM staff WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    return PasswordUtil.verifyPassword(password, storedHash);
                }
            }
        }
        return false;
    }
    
    public boolean updateSelf(int adminId, String newName, String newUser, String newEmail, String newPass, String adminRole) throws SQLException {
        boolean updatePassword = (newPass != null && !newPass.trim().isEmpty());
        
        String query = updatePassword ? 
            "UPDATE staff SET full_name = ?, username = ?, email = ?, password = ? WHERE staff_id = ?" :
            "UPDATE staff SET full_name = ?, username = ?, email = ? WHERE staff_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, newName);
            pstmt.setString(2, newUser);
            pstmt.setString(3, newEmail);
            
            if (updatePassword) {
                // SECURITY FIX: Hash the new password
                pstmt.setString(4, PasswordUtil.hashPassword(newPass));
                pstmt.setInt(5, adminId);
            } else {
                pstmt.setInt(4, adminId);
            }
            
            boolean success = pstmt.executeUpdate() > 0;
            if (success) {
                String details = "Staff member updated their own profile settings (Name/User/Email).";
                if (updatePassword) details += " [Password changed]";
                logService.record(adminId, adminRole, "Account Settings", details);
            }
            return success;
        }
    }
    
    // ==========================================================
    // ACCOUNT LOCKOUT METHODS (FIXED)
    // ==========================================================

    /**
     * Record a failed login attempt for staff
     */
    public void recordFailedLoginAttempt(String username) throws SQLException {
        String query = "UPDATE staff SET failed_login_attempts = failed_login_attempts + 1, " +
                       "account_locked = CASE WHEN failed_login_attempts + 1 >= 5 THEN 1 ELSE 0 END, " +
                       "lockout_time = CASE WHEN failed_login_attempts + 1 >= 5 THEN NOW() ELSE lockout_time END " +
                       "WHERE username = ? AND is_active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Check if a staff account is locked (FIXED: uses database time)
     */
    public boolean isAccountLocked(String username) throws SQLException {
        String query = "SELECT account_locked, lockout_time, " +
                       "TIMESTAMPDIFF(MINUTE, lockout_time, NOW()) as minutes_elapsed " +
                       "FROM staff WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean isLocked = rs.getInt("account_locked") == 1;
                    Timestamp lockoutTime = rs.getTimestamp("lockout_time");

                    // Auto-unlock after 30 minutes using database time
                    if (isLocked && lockoutTime != null) {
                        int minutesElapsed = rs.getInt("minutes_elapsed");
                        if (minutesElapsed >= 30) {
                            resetFailedLoginAttempts(username);
                            return false;
                        }
                    }
                    return isLocked;
                }
            }
        }
        return false;
    }

    /**
     * Reset failed login attempts (on successful login)
     */
    public void resetFailedLoginAttempts(String username) throws SQLException {
        String query = "UPDATE staff SET failed_login_attempts = 0, account_locked = 0, lockout_time = NULL WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Get remaining lockout time in minutes (FIXED: uses database time)
     */
    public int getRemainingLockoutMinutes(String username) throws SQLException {
        String query = "SELECT lockout_time, TIMESTAMPDIFF(MINUTE, lockout_time, NOW()) as minutes_elapsed " +
                       "FROM staff WHERE username = ? AND account_locked = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int minutesElapsed = rs.getInt("minutes_elapsed");
                    int remaining = 30 - minutesElapsed;
                    return Math.max(0, remaining);
                }
            }
        }
        return 0;
    }
    
     // ==========================================================
    // CROSS-TABLE USERNAME CHECK (for patient registration)
    // ==========================================================
    
    /**
     * Check if username exists in staff table (for cross-table validation)
     */
    public boolean isUsernameTakenInStaff(String username) throws SQLException {
        String query = "SELECT COUNT(*) FROM staff WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
