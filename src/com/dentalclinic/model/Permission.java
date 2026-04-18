package com.dentalclinic.model;

import java.util.Date;

public class Permission {
    private final int permissionId;
    private final String permissionName;
    private final String description;
    private final Date createdAt;
    private final Date updatedAt;

    public Permission(int permissionId, String permissionName, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.description = description;
        this.createdAt = null;
        this.updatedAt = null;
    }

    public Permission(int permissionId, String permissionName, String description, Date createdAt, Date updatedAt) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public String getPermissionName() {
        return permissionName;
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
}
