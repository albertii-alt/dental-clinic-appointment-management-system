package com.dentalclinic.dao;

import com.dentalclinic.model.Patient;
import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {
                    
    public boolean registerPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String pass) throws java.sql.SQLException {
    String query = "INSERT INTO patients (first_name, middle_name, last_name, birth_date, age, address, contact_number, email, username, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
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
        pstmt.setString(10, pass);
        
        return pstmt.executeUpdate() > 0;
    }
}
    // --- SYSTEM LOGIN (Admin, Dentist, Staff) ---
    // This removes hardcoded passwords from the UI
    public String checkSystemRole(String user, String pass) {
        if (user.equals("admin") && pass.equals("1234")) return "ADMIN";
        if (user.equals("dentist") && pass.equals("1234")) return "DENTIST";
        if (user.equals("staff") && pass.equals("1234")) return "STAFF";
        return null;
    }

    // --- PATIENT LOGIN ---
    public Patient login(String username, String password) throws SQLException {
        // 1. Added email to the SELECT query
        String query = "SELECT * FROM patients WHERE username = ? AND password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 2. Added rs.getString("email") to match the new constructor
                    return new Patient(
                        rs.getInt("patient_id"),
                        rs.getString("first_name"),
                        rs.getString("middle_name"),
                        rs.getString("last_name"),
                        rs.getDate("birth_date"),
                        rs.getInt("age"),
                        rs.getString("address"),
                        rs.getString("contact_number"),
                        rs.getString("email"), // This was missing!
                        rs.getString("username")
                    );
                }
            }
        }
        return null; 
    }
    
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
                        rs.getString("email"), // FETCH THE EMAIL HERE
                        rs.getString("username")
                    );
                }
            }
        }
        return null;
    }
    public List<Object[]> searchPatientsByName(String name) throws SQLException {
        List<Object[]> results = new ArrayList<>();
        // FIX: Changed 'dob' to 'birth_date' and 'contact_no' to 'contact_number'
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
                        rs.getDate("birth_date"), // Match the SELECT
                        rs.getString("address"),
                        rs.getString("contact_number") // Match the SELECT
                    });
                }
            }
        }
        return results;
    }
    
    
    public List<Object[]> getAllPatients() throws SQLException {
        List<Object[]> results = new ArrayList<>();
        String query = "SELECT patient_id, first_name, last_name, birth_date, address, contact_number FROM patients";

        try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

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
    
    // 1. Verify current password
    public boolean verifyPassword(int patientId, String currentPassword) throws SQLException {
        String query = "SELECT password FROM patients WHERE patient_id = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, patientId);
            pstmt.setString(2, currentPassword);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

// 2. Comprehensive Update
    public boolean updateFullProfile(int id, String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String newPass) throws SQLException {
        // Added 'email=?' to the query
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
            pstmt.setString(8, email); // New
            pstmt.setString(9, user);

            int paramIndex = 10;
            if (updatingPassword) {
                pstmt.setString(paramIndex++, newPass);
            }
            pstmt.setInt(paramIndex, id);

            return pstmt.executeUpdate() > 0;
        }
    }
    
}