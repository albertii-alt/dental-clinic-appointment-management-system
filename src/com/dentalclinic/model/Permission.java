package com.dentalclinic.model;

public class Permission {
    private final int permissionId;
    private final String permissionName;
    private final String description;

    public Permission(int permissionId, String permissionName, String description) {
        this.permissionId = permissionId;
        this.permissionName = permissionName;
        this.description = description;
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
}
