package com.dentalclinic.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {
    
    // BCrypt work factor (cost) - higher = more secure but slower
    // 10-12 is good for most applications
    private static final int BCRYPT_COST = 10;
    
    /**
     * Hash a plaintext password using BCrypt
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plainTextPassword.toCharArray());
    }
    
    /**
     * Verify a plaintext password against a BCrypt hash
     */
    public static boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        return BCrypt.verifyer().verify(plainTextPassword.toCharArray(), hashedPassword).verified;
    }
}