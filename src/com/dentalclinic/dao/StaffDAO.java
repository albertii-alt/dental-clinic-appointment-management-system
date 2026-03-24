package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StaffDAO {
    
    public Object[] login(String user, String pass, String role) throws SQLException {
    // Added full_name to the SELECT statement
        String query = "SELECT staff_id, role, is_super_admin, full_name FROM staff WHERE username = ? AND password = ? AND role = ? AND is_active = 1";

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
                        rs.getString("full_name") // New data at index 3
                    }; 
                }
            }
        }
        return null;
    }
    
    // 1. CREATE: Add New Staff
    public boolean addStaff(String name, String user, String pass, String email, String role) throws SQLException {
        String query = "INSERT INTO staff (full_name, username, password, email, role, is_active) VALUES (?, ?, ?, ?, ?, 1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, user);
            pstmt.setString(3, pass); // Note: In a production app, we would hash this!
            pstmt.setString(4, email);
            pstmt.setString(5, role);
            return pstmt.executeUpdate() > 0;
        }
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

    // 3. UPDATE: Update Existing Staff
    public boolean updateStaff(int id, String name, String user, String email, String role, String pass) throws SQLException {
    boolean updatePassword = (pass != null && !pass.trim().isEmpty());
    String query;
    
    if (updatePassword) {
        query = "UPDATE staff SET full_name = ?, username = ?, email = ?, role = ?, password = ? WHERE staff_id = ?";
    } else {
        query = "UPDATE staff SET full_name = ?, username = ?, email = ?, role = ? WHERE staff_id = ?";
    }

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setString(1, name);
        pstmt.setString(2, user);
        pstmt.setString(3, email);
        pstmt.setString(4, role);
        
        if (updatePassword) {
            pstmt.setString(5, pass);
            pstmt.setInt(6, id);
        } else {
            pstmt.setInt(5, id);
        }
        
        return pstmt.executeUpdate() > 0;
    }
}

    // 4. TOGGLE: Activate/Deactivate
    public boolean toggleStaffStatus(int id, boolean currentStatus) throws SQLException {
        String query = "UPDATE staff SET is_active = ? WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentStatus ? 0 : 1);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    // 5. DELETE: Permanent Removal
    public boolean deleteStaff(int id) throws SQLException {
        String query = "DELETE FROM staff WHERE staff_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}