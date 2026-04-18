package com.dentalclinic.model;

public class RolePermission {
    private int roleId;
    private int permissionId;
    private Role role;
    private Permission permission;

    public RolePermission(int roleId, int permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    public int getRoleId() { return roleId; }
    public int getPermissionId() { return permissionId; }
    public Role getRole() { return role; }
    public Permission getPermission() { return permission; }

    public void setRole(Role role) { this.role = role; }
    public void setPermission(Permission permission) { this.permission = permission; }
}
