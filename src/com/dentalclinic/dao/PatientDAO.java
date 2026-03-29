package com.dentalclinic.dao;

import com.dentalclinic.model.Patient;
import com.dentalclinic.util.DBConnection;
import com.dentalclinic.util.PasswordUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;

public class PatientDAO {
                    
    // ==========================================================
    // REGISTRATION & AUTHENTICATION
    // ==========================================================
    
    public boolean registerPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String pass) throws java.sql.SQLException {
        String query = "INSERT INTO patients (first_name, middle_name, last_name, birth_date, age, address, contact_number, email, username, password, force_password_reset) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
        
        try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, fName);
            pstmt.setString(2, mName);
            pstmt.setString(3, lName);
            pstmt.setDate(4, dob);
            pstmt.setInt(5, age);
            pstmt.setString(6, addr);
            pstmt.setString(7, phone);
            pstmt.setString(8, email);
            pstmt.setString(9, user);
            
            // SECURITY FIX: Hash the password before storing
            pstmt.setString(10, PasswordUtil.hashPassword(pass));
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public Patient login(String username, String password) throws SQLException {
        // SECURITY FIX: First get user by username only, then verify password hash
        String query = "SELECT * FROM patients WHERE username = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Get the stored password hash
                    String storedHash = rs.getString("password");
                    
                    // SECURITY FIX: Verify password against hash
                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        return new Patient(
                            rs.getInt("patient_id"),
                            rs.getString("first_name"),
                            rs.getString("middle_name"),
                            rs.getString("last_name"),
                            rs.getDate("birth_date"),
                            rs.getInt("age"),
                            rs.getString("address"),
                            rs.getString("contact_number"),
                            rs.getString("email"),
                            rs.getString("username")
                        );
                    }
                }
            }
        }
        return null;
    }
    
    // ==========================================================
    // PASSWORD RESET FLAG MANAGEMENT
    // ==========================================================
    
    public boolean needsPasswordReset(int patientId) throws SQLException {
        String query = "SELECT force_password_reset FROM patients WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("force_password_reset") == 1;
                }
            }
        }
        return false;
    }
    
    public void clearPasswordResetFlag(int patientId) throws SQLException {
        String query = "UPDATE patients SET force_password_reset = 0 WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patientId);
            pstmt.executeUpdate();
        }
    }
    
    // ==========================================================
    // PATIENT CRUD OPERATIONS
    // ==========================================================
    
    public Patient getPatientById(int id) throws SQLException {
        String query = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Patient(
                        rs.getInt("patient_id"), 
                        rs.getString("first_name"),
                        rs.getString("middle_name"), 
                        rs.getString("last_name"),
                        rs.getDate("birth_date"), 
                        rs.getInt("age"),
                        rs.getString("address"), 
                        rs.getString("contact_number"),
                        rs.getString("email"),
                        rs.getString("username")
                    );
                }
            }
        }
        return null;
    }
    
    public List<Object[]> searchPatientsByName(String name) throws SQLException {
        List<Object[]> results = new ArrayList<>();
        String query = "SELECT patient_id, first_name, last_name, birth_date, address, contact_number FROM patients " +
                       "WHERE first_name LIKE ? OR last_name LIKE ?";
        
        try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, "%" + name + "%");
            ps.setString(2, "%" + name + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new Object[]{
                        rs.getInt("patient_id"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getDate("birth_date"),
                        rs.getString("address"),
                        rs.getString("contact_number")
                    });
                }
            }
        }
        return results;
    }
    
    // SECURITY FIX: Changed from Statement to PreparedStatement
    public List<Object[]> getAllPatients() throws SQLException {
        List<Object[]> results = new ArrayList<>();
        String query = "SELECT patient_id, first_name, last_name, birth_date, address, contact_number FROM patients";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);  // Fixed!
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                results.add(new Object[]{
                    rs.getInt("patient_id"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getDate("birth_date"),
                    rs.getString("address"),
                    rs.getString("contact_number")
                });
            }
        }
        return results;
    }
    
    public boolean updatePatientProfile(int id, String address, String contact, String username) throws SQLException {
        String query = "UPDATE patients SET address = ?, contact_number = ?, username = ? WHERE patient_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, address);
            pstmt.setString(2, contact);
            pstmt.setString(3, username);
            pstmt.setInt(4, id);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    public boolean verifyPassword(int patientId, String currentPassword) throws SQLException {
        // SECURITY FIX: Get hash and verify, instead of direct comparison
        String query = "SELECT password FROM patients WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    return PasswordUtil.verifyPassword(currentPassword, storedHash);
                }
            }
        }
        return false;
    }
    
    public boolean updateFullProfile(int id, String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String newPass) throws SQLException {
        StringBuilder query = new StringBuilder("UPDATE patients SET first_name=?, middle_name=?, last_name=?, birth_date=?, age=?, address=?, contact_number=?, email=?, username=?");
        
        boolean updatingPassword = (newPass != null && !newPass.isEmpty());
        if (updatingPassword) query.append(", password=?");
        query.append(" WHERE patient_id=?");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            
            pstmt.setString(1, fName);
            pstmt.setString(2, mName);
            pstmt.setString(3, lName);
            pstmt.setDate(4, dob);
            pstmt.setInt(5, age);
            pstmt.setString(6, addr);
            pstmt.setString(7, phone);
            pstmt.setString(8, email);
            pstmt.setString(9, user);
            
            int paramIndex = 10;
            if (updatingPassword) {
                // SECURITY FIX: Hash the new password
                pstmt.setString(paramIndex++, PasswordUtil.hashPassword(newPass));
            }
            pstmt.setInt(paramIndex, id);
            
            return pstmt.executeUpdate() > 0;
        }
    }
    
    // ==========================================================
    // ACCOUNT LOCKOUT METHODS
    // ==========================================================

    /**
     * Record a failed login attempt for a patient
     */
    public void recordFailedLoginAttempt(String username) throws SQLException {
        // Remove is_active condition since patients table doesn't have it
        String query = "UPDATE patients SET failed_login_attempts = failed_login_attempts + 1, " +
                       "account_locked = CASE WHEN failed_login_attempts + 1 >= 5 THEN 1 ELSE 0 END, " +
                       "lockout_time = CASE WHEN failed_login_attempts + 1 >= 5 THEN NOW() ELSE lockout_time END " +
                       "WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Check if a patient account is locked
     */
    public boolean isAccountLocked(String username) throws SQLException {
        String query = "SELECT account_locked, lockout_time FROM patients WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean isLocked = rs.getInt("account_locked") == 1;
                    Timestamp lockoutTime = rs.getTimestamp("lockout_time");

                    // If locked and lockout time is more than 30 minutes ago, auto-unlock
                    if (isLocked && lockoutTime != null) {
                        long minutesSinceLockout = (System.currentTimeMillis() - lockoutTime.getTime()) / (1000 * 60);
                        if (minutesSinceLockout >= 30) {
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
        String query = "UPDATE patients SET failed_login_attempts = 0, account_locked = 0, lockout_time = NULL WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    /**
     * Get remaining lockout time in minutes
     */
    public int getRemainingLockoutMinutes(String username) throws SQLException {
        String query = "SELECT lockout_time FROM patients WHERE username = ? AND account_locked = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp lockoutTime = rs.getTimestamp("lockout_time");
                    if (lockoutTime != null) {
                        long minutesSinceLockout = (System.currentTimeMillis() - lockoutTime.getTime()) / (1000 * 60);
                        int remaining = 30 - (int) minutesSinceLockout;
                        return Math.max(0, remaining);
                    }
                }
            }
        }
        return 0;
    }
}