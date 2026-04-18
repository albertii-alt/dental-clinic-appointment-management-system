package com.dentalclinic.model;

import java.util.Date;

public class SystemLogEntry {
    private int systemLogId;
    private String logLevel;
    private String sourceClass;
    private String message;
    private Date timestamp;

    public SystemLogEntry(int systemLogId, String logLevel, String sourceClass, String message, Date timestamp) {
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
    public Date getTimestamp() { return timestamp; }
}
