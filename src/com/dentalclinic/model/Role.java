package com.dentalclinic.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Role {
    private final int roleId;
    private final String roleName;
    private final String description;
    private final Date createdAt;
    private final Date updatedAt;
    private List<Permission> permissions;

    public Role(int roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.description = null;
        this.createdAt = null;
        this.updatedAt = null;
        this.permissions = new ArrayList<>();
    }

    public Role(int roleId, String roleName, String description, Date createdAt, Date updatedAt) {
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }
}
