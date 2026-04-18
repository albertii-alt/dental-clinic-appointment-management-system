package com.dentalclinic.model;

import java.sql.Timestamp;

public class Permission {
    private final int permissionId;
    private final String permissionName;
    private final String description;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;

    public Permission(int permissionId, String permissionName, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.description = description;
        this.createdAt = null;
        this.updatedAt = null;
    }

    public Permission(int permissionId, String permissionName, String description, Timestamp createdAt, Timestamp updatedAt) {
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
}
