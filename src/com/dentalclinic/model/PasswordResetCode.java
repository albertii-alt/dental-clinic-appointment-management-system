package com.dentalclinic.model;

import java.sql.Timestamp;

public class PasswordResetCode {
    private int id;
    private String email;
    private String code;
    private String userType;
    private String username;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private boolean used;

    public PasswordResetCode(int id, String email, String code, String userType, String username, Timestamp createdAt, Timestamp expiresAt, boolean used) {
        this.id = id;
        this.email = email;
        this.code = code;
        this.userType = userType;
        this.username = username;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
    }

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getCode() { return code; }
    public String getUserType() { return userType; }
    public String getUsername() { return username; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
}
