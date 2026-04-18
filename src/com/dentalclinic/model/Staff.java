package com.dentalclinic.model;

import java.util.Date;

public class Staff {
    private int staffId;
    private String username;
    private String fullName;
    private String email;
    private int roleId;
    private String specialization;
    private String contactNumber;
    private Date createdAt;
    private boolean active;
    private boolean superAdmin;
    private boolean forcePasswordReset;
    private int failedLoginAttempts;
    private boolean accountLocked;
    private Date lockoutTime;
    private Role role;

    public Staff(int staffId, String username, String fullName, String email, int roleId) {
        this.staffId = staffId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.roleId = roleId;
    }

    public Staff(
            int staffId,
            String username,
            String fullName,
            String email,
            int roleId,
            String specialization,
            String contactNumber,
            Date createdAt,
            boolean active,
            boolean superAdmin,
            boolean forcePasswordReset,
            int failedLoginAttempts,
            boolean accountLocked,
            Date lockoutTime
    ) {
        this.staffId = staffId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.roleId = roleId;
        this.specialization = specialization;
        this.contactNumber = contactNumber;
        this.createdAt = createdAt;
        this.active = active;
        this.superAdmin = superAdmin;
        this.forcePasswordReset = forcePasswordReset;
        this.failedLoginAttempts = failedLoginAttempts;
        this.accountLocked = accountLocked;
        this.lockoutTime = lockoutTime;
    }

    public int getStaffId() { return staffId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public int getRoleId() { return roleId; }
    public String getSpecialization() { return specialization; }
    public String getContactNumber() { return contactNumber; }
    public Date getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public boolean isSuperAdmin() { return superAdmin; }
    public boolean isForcePasswordReset() { return forcePasswordReset; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public boolean isAccountLocked() { return accountLocked; }
    public Date getLockoutTime() { return lockoutTime; }
    public Role getRole() { return role; }

    public void setRole(Role role) { this.role = role; }
}
