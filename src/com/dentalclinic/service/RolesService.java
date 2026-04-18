package com.dentalclinic.service;

import com.dentalclinic.dao.RolesPermissionDAO;
import com.dentalclinic.model.Permission;
import com.dentalclinic.model.Role;
import com.dentalclinic.util.UserSession;
import java.sql.SQLException;
import java.util.List;

public class RolesService {
    private final RolesPermissionDAO rolesPermissionDAO = new RolesPermissionDAO();

    public boolean canManageRoles() {
        return UserSession.hasPermission("MANAGE_ROLES");
    }

    public List<Role> getAllRoles() throws SQLException {
        return rolesPermissionDAO.getAllRoles();
    }

    public List<Permission> getAllPermissions() {
        return rolesPermissionDAO.getAllPermissions();
    }

    public List<Integer> getPermissionIdsForRole(int roleId) {
        return rolesPermissionDAO.getPermissionIdsForRole(roleId);
    }

    public int getRoleIdByName(String roleName) throws SQLException {
        return rolesPermissionDAO.getRoleIdByName(roleName);
    }

    public List<String> getPermissionNamesForRole(int roleId) {
        return rolesPermissionDAO.getPermissionNamesForRole(roleId);
    }

    public boolean updateRolePermissions(int roleId, List<Integer> permissionIds) {
        return rolesPermissionDAO.updateRolePermissions(roleId, permissionIds);
    }
}
