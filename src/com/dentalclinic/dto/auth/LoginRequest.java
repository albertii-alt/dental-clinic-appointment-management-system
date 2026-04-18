package com.dentalclinic.dto.auth;

public class LoginRequest {
    private final String username;
    private final String password;
    private final String selectedRole;

    public LoginRequest(String username, String password, String selectedRole) {
        this.username = username;
        this.password = password;
        this.selectedRole = selectedRole;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSelectedRole() {
        return selectedRole;
    }
}
