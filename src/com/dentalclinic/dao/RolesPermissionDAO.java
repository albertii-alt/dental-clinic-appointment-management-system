package com.dentalclinic.dao;

import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RolesPermissionDAO {

    // 1. Fetch all permissions a specific role has (Used for the UI Checklist)
    public List<Integer> getPermissionIdsForRole(int roleId) {
        List<Integer> permissionIds = new ArrayList<>();
        String query = "SELECT permission_id FROM role_permissions WHERE role_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, roleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                permissionIds.add(rs.getInt("permission_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return permissionIds;
    }

    // 2. Fetch all available permissions in the system (Used to populate the UI)
    public List<Permission> getAllPermissions() {
        List<Permission> list = new ArrayList<>();
        String query = "SELECT * FROM permissions";
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                list.add(new Permission(
                    rs.getInt("permission_id"),
                    rs.getString("permission_name"),
                    rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. THE HARD PART: Update Role Permissions using a TRANSACTION
    public boolean updateRolePermissions(int roleId, List<Integer> newPermissionIds) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // START TRANSACTION

            // Step A: Delete all existing permissions for this role
            String deleteSQL = "DELETE FROM role_permissions WHERE role_id = ?";
            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSQL)) {
                deletePstmt.setInt(1, roleId);
                deletePstmt.executeUpdate();
            }

            // Step B: Insert the new set of permissions
            String insertSQL = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)";
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSQL)) {
                for (int permId : newPermissionIds) {
                    insertPstmt.setInt(1, roleId);
                    insertPstmt.setInt(2, permId);
                    insertPstmt.addBatch(); // Use batch for better performance
                }
                insertPstmt.executeBatch();
            }

            conn.commit(); // SAVE CHANGES
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    // Simple Helper Class for the UI
    public static class Permission {
        public int id;
        public String name;
        public String description;

        public Permission(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
        @Override
        public String toString() { return name; } // Shows name in UI lists
    }
    
    // Inside RolesPermissionDAO.java
    public List<String> getPermissionNamesForRole(int roleId) {
        List<String> permissionNames = new ArrayList<>();
        String query = "SELECT p.permission_name FROM permissions p " +
                       "JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                       "WHERE rp.role_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, roleId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                permissionNames.add(rs.getString("permission_name"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return permissionNames;
    }
}
