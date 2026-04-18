package com.dentalclinic.model;

import java.util.Date;

public class PasswordResetCode {
    private int id;
    private String email;
    private String code;
    private String userType;
    private String username;
    private Date createdAt;
    private Date expiresAt;
    private boolean used;

    public PasswordResetCode(int id, String email, String code, String userType, String username, Date createdAt, Date expiresAt, boolean used) {
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
    public Date getCreatedAt() { return createdAt; }
    public Date getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
}
