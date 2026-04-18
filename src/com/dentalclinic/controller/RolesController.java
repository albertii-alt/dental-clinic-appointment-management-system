package com.dentalclinic.controller;

import com.dentalclinic.model.Permission;
import com.dentalclinic.model.Role;
import com.dentalclinic.service.RolesService;
import java.sql.SQLException;
import java.util.List;

public class RolesController {
    private final RolesService rolesService = new RolesService();

    public boolean canCurrentUserManageRoles() {
        return rolesService.canManageRoles();
    }

    public List<Role> getAllRoles() throws SQLException {
        return rolesService.getAllRoles();
    }

    public List<Permission> getAllPermissions() {
        return rolesService.getAllPermissions();
    }

    public List<Integer> getPermissionIdsForRole(int roleId) {
        return rolesService.getPermissionIdsForRole(roleId);
    }

    public boolean saveRolePermissions(int roleId, List<Integer> permissionIds) {
        return rolesService.updateRolePermissions(roleId, permissionIds);
    }
}
