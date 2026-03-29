package com.dentalclinic.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class UserSession {
    private static int userId;
    private static String fullName;
    private static String role;
    private static Set<String> permissions = new HashSet<>();
    
    // Session timeout tracking
    private static long lastActivityTime;
    private static Timer sessionTimer;
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    
    // Session listeners (for UI components that need to know about timeout)
    private static List<SessionTimeoutListener> listeners;
    
    public interface SessionTimeoutListener {
        void onSessionTimeout();
    }
    
    public static void initialize(int id, String name, String userRole, List<String> userPermissions) {
        userId = id;
        fullName = name;
        role = userRole;
        permissions.clear();
        if (userPermissions != null) {
            permissions.addAll(userPermissions);
        }
        
        // Initialize session activity timer
        updateActivity();
        startSessionTimer();
    }
    
    /**
     * Call this method on every user action (button click, page navigation, etc.)
     * to keep session alive
     */
    public static void updateActivity() {
        lastActivityTime = System.currentTimeMillis();
    }
    
    /**
     * Check if session is still valid (not timed out)
     */
    public static boolean isSessionValid() {
        if (userId == 0) {
            return false; // Not logged in
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastActivityTime;
        
        if (elapsed > SESSION_TIMEOUT_MS) {
            // Session expired
            logout();
            return false;
        }
        
        return true;
    }
    
    /**
     * Start the session timer that checks for inactivity
     */
    private static void startSessionTimer() {
        if (sessionTimer != null) {
            sessionTimer.cancel();
        }
        
        sessionTimer = new Timer(true);
        sessionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (userId != 0 && !isSessionValid()) {
                    // Session expired - notify listeners if any
                    if (listeners != null) {
                        for (SessionTimeoutListener listener : listeners) {
                            listener.onSessionTimeout();
                        }
                    }
                }
            }
        }, 60000, 60000); // Check every minute
    }
    
    /**
     * Add listener for session timeout events
     */
    public static void addSessionTimeoutListener(SessionTimeoutListener listener) {
        if (listeners == null) {
            listeners = new java.util.ArrayList<>();
        }
        listeners.add(listener);
    }
    
    /**
     * Check if user has specific permission
     * FIXED: Better permission normalization
     */
    public static boolean hasPermission(String permissionName) {
        // Super Admin bypass
        if ("Super Admin".equalsIgnoreCase(role)) {
            return true;
        }
        
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        // Normalize: Remove spaces, convert to uppercase
        String normalizedSearch = permissionName.toUpperCase().replace(" ", "_");
        
        for (String p : permissions) {
            // Normalize stored permission (remove spaces, uppercase)
            String normalizedStored = p.toUpperCase().replace(" ", "_");
            if (normalizedStored.equals(normalizedSearch)) {
                return true;
            }
        }
        return false;
    }
    
    public static int getUserId() { 
        updateActivity(); // Activity tracking
        return userId; 
    }
    
    public static String getFullName() { 
        updateActivity();
        return fullName; 
    }
    
    public static String getUserRole() { 
        updateActivity();
        return role; 
    }
    
    public static Set<String> getPermissions() {
        updateActivity();
        return new HashSet<>(permissions); // Return copy to prevent modification
    }
    
    /**
     * Get remaining session time in minutes
     */
    public static long getRemainingSessionMinutes() {
        if (userId == 0) return 0;
        long elapsed = System.currentTimeMillis() - lastActivityTime;
        long remaining = SESSION_TIMEOUT_MS - elapsed;
        return Math.max(0, remaining / 60000);
    }
    
    public static void logout() {
        // Cancel timer
        if (sessionTimer != null) {
            sessionTimer.cancel();
            sessionTimer = null;
        }
        
        // Clear all session data
        userId = 0;
        fullName = null;
        role = null;
        permissions.clear();
        lastActivityTime = 0;
        
        // Clear listeners
        if (listeners != null) {
            listeners.clear();
        }
    }
}