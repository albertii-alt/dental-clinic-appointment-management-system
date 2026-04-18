package com.dentalclinic.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Role {
    private final int roleId;
    private final String roleName;
    private final String description;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    private List<Permission> permissions;

    public Role(int roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = null;
        this.createdAt = null;
        this.updatedAt = null;
        this.permissions = new ArrayList<>();
    }

    public Role(int roleId, String roleName, String description, Timestamp createdAt, Timestamp updatedAt) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.permissions = new ArrayList<>();
    }

    public int getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }
}
