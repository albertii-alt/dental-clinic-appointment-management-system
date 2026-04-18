package com.dentalclinic.dao;

import com.dentalclinic.model.Permission;
import com.dentalclinic.model.Role;
import com.dentalclinic.util.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RolesPermissionDAO {

    // 1. Fetch all permissions a specific role has
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

    // 2. Fetch all available permissions (SECURITY FIX: Use PreparedStatement)
    public List<Permission> getAllPermissions() {
        List<Permission> list = new ArrayList<>();
        String query = "SELECT * FROM permissions";
        
        // SECURITY FIX: Changed from Statement to PreparedStatement
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
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

    public List<Role> getAllRoles() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT role_id, role_name FROM roles ORDER BY role_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                roles.add(new Role(rs.getInt("role_id"), rs.getString("role_name")));
            }
        }
        return roles;
    }

    // 3. Update Role Permissions using TRANSACTION
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
                    insertPstmt.addBatch();
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

    // Get permission names as strings for UserSession
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
    
        public int getRoleIdByName(String roleName) throws SQLException {
        String query = "SELECT role_id FROM roles WHERE UPPER(role_name) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roleName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("role_id");
                }
            }
        }
        // Fallback hardcoded mapping if database query fails
        if (roleName.equalsIgnoreCase("ADMIN")) return 1;
        if (roleName.equalsIgnoreCase("DENTIST")) return 2;
        if (roleName.equalsIgnoreCase("STAFF")) return 3;
        if (roleName.equalsIgnoreCase("PATIENT")) return 4;
        return -1; // Not found
    }
}
