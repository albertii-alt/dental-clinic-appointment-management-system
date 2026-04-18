package com.dentalclinic.model;

import java.sql.Timestamp;

public class ActivityLogEntry {
    private int logId;
    private int userId;
    private String userRole;
    private String action;
    private String details;
    private Timestamp timestamp;

    public ActivityLogEntry(int logId, int userId, String userRole, String action, String details, Timestamp timestamp) {
        this.logId = logId;
        this.userId = userId;
        this.userRole = userRole;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getLogId() { return logId; }
    public int getUserId() { return userId; }
    public String getUserRole() { return userRole; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public Timestamp getTimestamp() { return timestamp; }
}
