package com.dentalclinic.util;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Comprehensive XSS Prevention and Input Sanitization Utility
 * 
 * This class provides methods to sanitize user inputs before saving to database
 * and escape outputs before displaying in UI components.
 * 
 * Usage:
 * - For database storage: Sanitizer.sanitizeForInput(userInput)
 * - For HTML display: Sanitizer.escapeForHTML(userInput)
 * - For text fields: Sanitizer.sanitizeTextField(userInput)
 */
public final class Sanitizer {
    
    // Private constructor to prevent instantiation
    private Sanitizer() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    // ==========================================================
    // PATTERNS FOR DETECTION
    // ==========================================================
    
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("(?i)<script.*?>.*?</script>");
    private static final Pattern EVENT_HANDLER_PATTERN = Pattern.compile("(?i)on\\w+\\s*=");
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile("(?i)javascript\\s*:");
    private static final Pattern VBSCRIPT_PROTOCOL_PATTERN = Pattern.compile("(?i)vbscript\\s*:");
    private static final Pattern CSS_EXPRESSION_PATTERN = Pattern.compile("(?i)expression\\s*\\(");
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("(?i)data\\s*:.*?base64");
    private static final Pattern URL_ENCODED_SCRIPT_PATTERN = Pattern.compile("%3[cC]|%3[eE]|%3[sS]|%63|%67|%73");
    private static final Pattern UNICODE_SCRIPT_PATTERN = Pattern.compile("\\\\u003c|\\\\u003e|\\\\u0063|\\\\u0073");
    private static final Pattern NULL_BYTE_PATTERN = Pattern.compile("\\x00");
    private static final Pattern NULL_CHAR_PATTERN = Pattern.compile("%00");
    
    // Allowed characters for different contexts
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.!?,@#$%^&*()\\[\\]{};:'\"+/=<>~`|\\\\]*$");
    
    // Email validation pattern (RFC 5322 compliant simplified)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // Phone validation pattern (international format support)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-()]{8,20}$");
    
    // Name validation pattern (letters, spaces, hyphens, apostrophes)
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s\\-']+$");
    
    // Username validation pattern (alphanumeric, underscore, dot, hyphen)
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,50}$");
    
    // ==========================================================
    // CORE SANITIZATION METHODS
    // ==========================================================
    
    /**
     * Primary sanitization method for text input before database storage.
     * Removes all potentially dangerous content while preserving safe text.
     * 
     * @param input Raw user input
     * @return Sanitized string safe for database storage
     */
    public static String sanitizeForInput(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String sanitized = input;
        
        // 1. Remove null bytes and control characters
        sanitized = NULL_BYTE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = NULL_CHAR_PATTERN.matcher(sanitized).replaceAll("");
        
        // 2. Remove script tags completely
        sanitized = SCRIPT_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // 3. Remove HTML tags (keeps text content)
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // 4. Remove JavaScript protocol
        sanitized = JAVASCRIPT_PROTOCOL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = VBSCRIPT_PROTOCOL_PATTERN.matcher(sanitized).replaceAll("");
        
        // 5. Remove CSS expressions
        sanitized = CSS_EXPRESSION_PATTERN.matcher(sanitized).replaceAll("");
        
        // 6. Remove event handlers
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");
        
        // 7. Decode and remove URL encoded attacks
        sanitized = URL_ENCODED_SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // 8. Remove Unicode encoded attacks
        sanitized = UNICODE_SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // 9. Remove data URLs
        sanitized = DATA_URL_PATTERN.matcher(sanitized).replaceAll("");
        
        // 10. Basic HTML entity encoding for remaining special characters
        sanitized = escapeHtmlEntities(sanitized);
        
        // 11. Trim whitespace
        sanitized = sanitized.trim();
        
        return sanitized;
    }
    
    /**
     * Escape string for safe display in HTML context (JEditorPane, JLabel with HTML).
     * Converts special characters to HTML entities.
     * 
     * @param input String to escape
     * @return HTML-escaped string
     */
    public static String escapeForHTML(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '&':
                    escaped.append("&amp;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                case '/':
                    escaped.append("&#47;");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }
    
    /**
     * Escape string for safe use in JavaScript string context.
     * 
     * @param input String to escape
     * @return JavaScript-escaped string
     */
    public static String escapeForJavaScript(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\'':
                    escaped.append("\\'");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '<':
                    escaped.append("\\x3C");
                    break;
                case '>':
                    escaped.append("\\x3E");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }
    
    /**
     * Sanitize text field input (preserves some formatting).
     * Use this for multi-line text fields like addresses, descriptions.
     * 
     * @param input Raw input from text field
     * @return Sanitized text safe for storage
     */
    public static String sanitizeTextField(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String sanitized = sanitizeForInput(input);
        
        // Allow newlines and basic punctuation
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        
        return sanitized;
    }
    
    /**
     * Sanitize a name (patient name, staff name, etc.)
     * Allows letters, spaces, hyphens, apostrophes.
     * 
     * @param input Name input
     * @return Sanitized name
     */
    public static String sanitizeName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String sanitized = sanitizeForInput(input);
        
        // Remove anything not allowed in a name
        sanitized = sanitized.replaceAll("[^a-zA-Z\\s\\-']", "");
        
        // Normalize spaces
        sanitized = sanitized.trim().replaceAll("\\s+", " ");
        
        // Capitalize first letter of each word
        String[] words = sanitized.split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                capitalized.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase())
                          .append(" ");
            }
        }
        
        return capitalized.toString().trim();
    }
    
    /**
     * Sanitize a username.
     * Allows alphanumeric, underscore, dot, hyphen. Min 3, max 50 chars.
     * 
     * @param input Username input
     * @return Sanitized username
     */
    public static String sanitizeUsername(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String sanitized = sanitizeForInput(input);
        
        // Keep only allowed characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_.-]", "");
        
        // Trim to length limits
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize email address.
     * 
     * @param input Email input
     * @return Sanitized email (or empty string if invalid)
     */
    public static String sanitizeEmail(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String sanitized = sanitizeForInput(input);
        
        // Remove anything not allowed in email
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9@._+-]", "");
        
        // Convert to lowercase
        sanitized = sanitized.toLowerCase();
        
        // Validate email format
        if (isValidEmail(sanitized)) {
            return sanitized;
        }
        
        return "";
    }
    
    /**
     * Sanitize phone number.
     * Keeps digits, spaces, hyphens, parentheses, plus sign.
     * 
     * @param input Phone input
     * @return Sanitized phone number
     */
    public static String sanitizePhone(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String sanitized = sanitizeForInput(input);
        
        // Keep only phone-valid characters
        sanitized = sanitized.replaceAll("[^0-9\\s\\-()+]", "");
        
        // Trim excess whitespace
        sanitized = sanitized.trim();
        
        return sanitized;
    }
    
    /**
     * Sanitize a file name (remove path traversal and dangerous chars).
     * 
     * @param input File name input
     * @return Sanitized file name
     */
    public static String sanitizeFileName(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Remove path traversal attempts
        String sanitized = input.replaceAll("\\.\\./", "")
                                .replaceAll("\\.\\.\\\\", "")
                                .replaceAll("\\/", "")
                                .replaceAll("\\\\", "");
        
        // Remove dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "");
        
        // Prevent empty file names
        if (sanitized.isEmpty()) {
            sanitized = "file";
        }
        
        return sanitized;
    }
    
    /**
     * Sanitize numeric input (ensures only digits).
     * 
     * @param input Numeric input
     * @return String containing only digits (or empty string)
     */
    public static String sanitizeNumeric(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        return input.replaceAll("[^0-9]", "");
    }
    
    /**
     * Sanitize decimal input (ensures only digits and optional decimal).
     * 
     * @param input Decimal input
     * @return Sanitized decimal string
     */
    public static String sanitizeDecimal(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Allow digits and single decimal point
        String sanitized = input.replaceAll("[^0-9.]", "");
        
        // Ensure only one decimal point
        int decimalIndex = sanitized.indexOf('.');
        if (decimalIndex != -1) {
            String before = sanitized.substring(0, decimalIndex);
            String after = sanitized.substring(decimalIndex + 1).replaceAll("\\.", "");
            sanitized = before + "." + after;
        }
        
        return sanitized;
    }
    
    // ==========================================================
    // VALIDATION METHODS
    // ==========================================================
    
    /**
     * Check if input contains potential XSS patterns.
     * 
     * @param input String to check
     * @return true if XSS pattern detected
     */
    public static boolean hasXSSPattern(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return SCRIPT_TAG_PATTERN.matcher(input).find() ||
               EVENT_HANDLER_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PROTOCOL_PATTERN.matcher(input).find() ||
               HTML_TAG_PATTERN.matcher(input).find();
    }
    
    /**
     * Validate email format.
     * 
     * @param email Email to validate
     * @return true if email format is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate phone number format.
     * 
     * @param phone Phone to validate
     * @return true if phone format is valid
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Validate name format (only letters, spaces, hyphens, apostrophes).
     * 
     * @param name Name to validate
     * @return true if name format is valid
     */
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * Validate username format.
     * 
     * @param username Username to validate
     * @return true if username format is valid
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * Validate address (basic - not empty, reasonable length).
     * 
     * @param address Address to validate
     * @return true if address is valid
     */
    public static boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        return address.length() >= 5 && address.length() <= 500;
    }
    
    // ==========================================================
    // PRIVATE HELPER METHODS
    // ==========================================================
    
    private static String escapeHtmlEntities(String input) {
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                escaped.append("&#").append((int) c).append(";");
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
    
    // ==========================================================
    // BATCH SANITIZATION FOR DATA OBJECTS
    // ==========================================================
    
    /**
     * Sanitize all string fields in a Patient object before saving.
     * 
     * @param firstName Patient first name
     * @param middleName Patient middle name
     * @param lastName Patient last name
     * @param address Patient address
     * @param phone Patient phone
     * @param email Patient email
     * @param username Patient username
     * @return Array of sanitized values [firstName, middleName, lastName, address, phone, email, username]
     */
    public static String[] sanitizePatientData(String firstName, String middleName, String lastName,
                                                String address, String phone, String email, String username) {
        return new String[] {
            sanitizeName(firstName),
            sanitizeName(middleName),
            sanitizeName(lastName),
            sanitizeTextField(address),
            sanitizePhone(phone),
            sanitizeEmail(email),
            sanitizeUsername(username)
        };
    }
    
    /**
     * Sanitize all string fields in a Staff object before saving.
     * 
     * @param fullName Staff full name
     * @param username Staff username
     * @param email Staff email
     * @return Array of sanitized values [fullName, username, email]
     */
    public static String[] sanitizeStaffData(String fullName, String username, String email) {
        return new String[] {
            sanitizeName(fullName),
            sanitizeUsername(username),
            sanitizeEmail(email)
        };
    }
    
    /**
     * Sanitize service data.
     * 
     * @param serviceName Service name
     * @param description Service description
     * @return Array of sanitized values [serviceName, description]
     */
    public static String[] sanitizeServiceData(String serviceName, String description) {
        return new String[] {
            sanitizeTextField(serviceName),
            sanitizeTextField(description)
        };
    }
}