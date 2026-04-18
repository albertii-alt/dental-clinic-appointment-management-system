package com.dentalclinic.dto.auth;

import com.dentalclinic.model.Patient;
import java.util.List;

public class LoginResult {
    public enum Status {
        SUCCESS_STAFF,
        SUCCESS_PATIENT,
        ACCOUNT_LOCKED,
        RESET_REQUIRED,
        INVALID_CREDENTIALS
    }

    private final Status status;
    private final int userId;
    private final String roleName;
    private final boolean superAdmin;
    private final String fullName;
    private final String email;
    private final Patient patient;
    private final int remainingMinutes;
    private final List<String> permissions;

    public LoginResult(Status status, int userId, String roleName, boolean superAdmin, String fullName, String email,
                       Patient patient, int remainingMinutes, List<String> permissions) {
        this.status = status;
        this.userId = userId;
        this.roleName = roleName;
        this.superAdmin = superAdmin;
        this.fullName = fullName;
        this.email = email;
        this.patient = patient;
        this.remainingMinutes = remainingMinutes;
        this.permissions = permissions;
    }

    public Status getStatus() {
        return status;
    }

    public int getUserId() {
        return userId;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Patient getPatient() {
        return patient;
    }

    public int getRemainingMinutes() {
        return remainingMinutes;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
