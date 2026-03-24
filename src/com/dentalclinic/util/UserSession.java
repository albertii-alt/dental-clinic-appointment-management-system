package com.dentalclinic.util;

public class UserSession {
    private static int userId;
    private static String fullName;
    private static String role;

    // Call this right after a successful login
    public static void initialize(int id, String name, String userRole) {
        userId = id;
        fullName = name;
        role = userRole;
    }

    public static int getUserId() { return userId; }
    public static String getFullName() { return fullName; }
    public static String getUserRole() { return role; }

    public static void logout() {
        userId = 0;
        fullName = null;
        role = null;
    }
}