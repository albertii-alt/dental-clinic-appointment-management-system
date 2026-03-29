package com.dentalclinic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PasswordValidator {
    
    // Password requirements
    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    
    // Patterns for validation
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    
    /**
     * Validate password strength
     * @return List of error messages (empty if valid)
     */
    public static List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        
        if (password == null || password.isEmpty()) {
            errors.add("Password cannot be empty");
            return errors;
        }
        
        // Check length
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("Password must be less than " + MAX_LENGTH + " characters");
        }
        
        // FIX: Check for spaces - simple and reliable
        if (password.contains(" ")) {
            errors.add("Password cannot contain spaces");
        }
        
        // Check complexity
        if (!HAS_UPPERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (!HAS_LOWERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            errors.add("Password must contain at least one number");
        }
        if (!HAS_SPECIAL.matcher(password).find()) {
            errors.add("Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)");
        }
        
        // Check for common weak passwords
        String lowerPassword = password.toLowerCase();
        String[] weakPasswords = {"password", "123456", "12345678", "qwerty", "abc123", 
                                   "admin", "welcome", "letmein", "password123"};
        for (String weak : weakPasswords) {
            if (lowerPassword.equals(weak)) {
                errors.add("Password is too common. Please choose a stronger password");
                break;
            }
        }
        
        return errors;
    }
    
    /**
     * Check if password meets requirements
     */
    public static boolean isValid(String password) {
        return validatePassword(password).isEmpty();
    }
    
    /**
     * Get password requirements as a readable string
     */
    public static String getRequirements() {
        return "Password must be " + MIN_LENGTH + "-" + MAX_LENGTH + " characters, " +
               "contain uppercase, lowercase, number, special character (!@#$%^&*()), " +
               "and no spaces.";
    }
}