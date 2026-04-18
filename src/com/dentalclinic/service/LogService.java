package com.dentalclinic.service;

import com.dentalclinic.dao.LogDAO;
import java.sql.SQLException;
import java.util.List;

public class LogService {
    private LogDAO logDAO = new LogDAO();

    // ==========================================================
    // SECTION 1: USER ACTIVITY LOGS (Human Actions)
    // ==========================================================
    
    // The method to call whenever someone does something important
    public void record(int userId, String role, String action, String details) {
        logDAO.insertLog(userId, role, action, details);
    }

    public List<Object[]> getActivityLogs() throws SQLException {
        return logDAO.getAllLogsWithNames();
    }

    // ==========================================================
    // SECTION 2: SYSTEM LOGS (Technical/Background Events)
    // ==========================================================

    /**
     * STATIC Utility to record system events. 
     * Can be called from anywhere: LogService.logSystemEvent(...)
     */
    public static void logSystemEvent(String level, String source, String message) {
        try {
            new LogDAO().insertSystemLog(level, source, message);
        } catch (Exception e) {
            // Fallback to console if DB is unreachable
            System.err.println("CRITICAL: Could not write to system_logs table!");
            System.err.println("Log attempted: [" + level + "] " + source + ": " + message);
            e.printStackTrace();
        }
    }

    public List<Object[]> getSystemLogs() throws Exception {
        return logDAO.getSystemLogs();
    }
    
    public boolean clearAllSystemLogs(int staffId, String role) {
        // 1. First, we record WHO is doing this in the Audit Trail (activity_logs)
        // This happens BEFORE the deletion so the record is safe.
        record(staffId, role, "System Maintenance", "Permanently cleared all technical System Logs.");

        try {
            logDAO.clearAllSystemLogs();
            return true;
        } catch (Exception e) {
            System.err.println("Error clearing system logs: " + e.getMessage());
            return false;
        }
    }

    
    // Add this to your LogService.java
    public boolean verifySuperAdminPassword(int adminId, String password) {
        try {
            return logDAO.verifySuperAdminPassword(adminId, password);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean archiveActivityLogs(int staffId, String role) {
        // We do NOT call record() here yet because the table is about to be wiped.
        // We will record the action in the UI controller after the wipe to ensure it's the "First" new entry.
        try {
            logDAO.archiveActivityLogs();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
