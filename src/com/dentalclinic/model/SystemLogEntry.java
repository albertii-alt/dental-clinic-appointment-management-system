package com.dentalclinic.model;

import java.sql.Timestamp;

public class SystemLogEntry {
    private int systemLogId;
    private String logLevel;
    private String sourceClass;
    private String message;
    private Timestamp timestamp;

    public SystemLogEntry(int systemLogId, String logLevel, String sourceClass, String message, Timestamp timestamp) {
        this.systemLogId = systemLogId;
        this.logLevel = logLevel;
        this.sourceClass = sourceClass;
        this.message = message;
        this.timestamp = timestamp;
    }

    public int getSystemLogId() { return systemLogId; }
    public String getLogLevel() { return logLevel; }
    public String getSourceClass() { return sourceClass; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
}
