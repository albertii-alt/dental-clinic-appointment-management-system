package com.dentalclinic.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserSession {
    private static int userId;
    private static String fullName;
    private static String role;
    private static Set<String> permissions = new HashSet<>(); // ADD THIS

    public static void initialize(int id, String name, String userRole, List<String> userPermissions) {
        userId = id;
        fullName = name;
        role = userRole;
        permissions.clear();
        if (userPermissions != null) {
            permissions.addAll(userPermissions);
        }
    }

    // THE CORE SECURITY CHECK: Call this in your dashboards
    public static boolean hasPermission(String permissionName) {
        // --- SUPER ADMIN BYPASS ---
        // If the role is Super Admin, they always have permission.
        if ("Super Admin".equalsIgnoreCase(role)) {
            return true;
        }

        // --- REGULAR PERMISSION CHECK ---
        if (permissions == null) return false;

        String search = permissionName.toUpperCase().replace(" ", "_");

        for (String p : permissions) {
            String normalizedDbValue = p.toUpperCase().replace(" ", "_");
            if (normalizedDbValue.equals(search)) {
                return true;
            }
        }
        return false;
    }

    public static int getUserId() { return userId; }
    public static String getFullName() { return fullName; }
    public static String getUserRole() { return role; }

    public static void logout() {
        userId = 0;
        fullName = null;
        role = null;
        permissions.clear();
    }
}