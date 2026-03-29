package com.dentalclinic.util;

import java.sql.*;
import java.util.*;

public class PasswordMigration {
    
    public static void main(String[] args) {
        System.out.println("Starting password migration...");
        System.out.println("WARNING: This will convert all plaintext passwords to BCrypt hashes.");
        System.out.println("Please backup your database before proceeding!");
        
        try {
            migratePatientPasswords();
            migrateStaffPasswords();
            System.out.println("Password migration completed successfully!");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void migratePatientPasswords() throws SQLException {
        String query = "SELECT patient_id, password FROM patients WHERE password NOT LIKE '$2a$%'";
        List<Object[]> toUpdate = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                int id = rs.getInt("patient_id");
                String plainPassword = rs.getString("password");
                String hashedPassword = PasswordUtil.hashPassword(plainPassword);
                toUpdate.add(new Object[]{id, hashedPassword});
            }
        }
        
        // Update with hashed passwords
        String updateQuery = "UPDATE patients SET password = ? WHERE patient_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            
            for (Object[] data : toUpdate) {
                pstmt.setString(1, (String) data[1]);
                pstmt.setInt(2, (Integer) data[0]);
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            System.out.println("Migrated " + results.length + " patient passwords");
        }
    }
    
    private static void migrateStaffPasswords() throws SQLException {
        String query = "SELECT staff_id, password FROM staff WHERE password NOT LIKE '$2a$%'";
        List<Object[]> toUpdate = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                int id = rs.getInt("staff_id");
                String plainPassword = rs.getString("password");
                String hashedPassword = PasswordUtil.hashPassword(plainPassword);
                toUpdate.add(new Object[]{id, hashedPassword});
            }
        }
        
        // Update with hashed passwords
        String updateQuery = "UPDATE staff SET password = ? WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            
            for (Object[] data : toUpdate) {
                pstmt.setString(1, (String) data[1]);
                pstmt.setInt(2, (Integer) data[0]);
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            System.out.println("Migrated " + results.length + " staff passwords");
        }
    }
}