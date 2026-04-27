package com.dentalclinic.service;

import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.dao.StaffDAO;
import com.dentalclinic.dao.PasswordResetDAO;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.DBConnection;
import com.dentalclinic.util.EmailUtil;
import com.dentalclinic.util.PasswordUtil;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.dentalclinic.util.PasswordValidator;
import java.util.List;

public class AuthService {
    private PatientDAO patientDAO = new PatientDAO();
    private StaffDAO staffDAO = new StaffDAO();
    private PasswordResetDAO passwordResetDAO = new PasswordResetDAO();

    public boolean isDatabaseAvailable() {
        // TCP socket check works on both Linux and Windows without elevated privileges.
        // InetAddress.isReachable() uses ICMP which requires root on Linux — avoid it.
        // Timeout is 5s to account for slower DNS resolution on Windows.
        // If socket check fails, fall through to a full JDBC connection test
        // before giving up — avoids false negatives on Windows cold starts.
        try {
            String[] hostPort = extractHostAndPort();
            if (hostPort != null) {
                try (java.net.Socket socket = new java.net.Socket()) {
                    socket.connect(new java.net.InetSocketAddress(hostPort[0], Integer.parseInt(hostPort[1])), 5000);
                    return DBConnection.testConnection();
                } catch (Exception socketEx) {
                    // Socket failed — try a full JDBC connection as fallback
                    return DBConnection.testConnection();
                }
            }
        } catch (Exception e) {
            return false;
        }
        return DBConnection.testConnection();
    }

    private String[] extractHostAndPort() {
        try {
            String path = System.getProperty("user.home") + java.io.File.separator + ".dental_clinic" + java.io.File.separator + "db.properties";
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream(path)) {
                props.load(fis);
            }
            String dbUrl = props.getProperty("db.url", "");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("jdbc:mysql://([^:/]+):(\\d+)").matcher(dbUrl);
            if (m.find()) return new String[]{m.group(1), m.group(2)};
            // No port in URL — try host-only match with default port 3306
            java.util.regex.Matcher mHost = java.util.regex.Pattern.compile("jdbc:mysql://([^:/]+)").matcher(dbUrl);
            return mHost.find() ? new String[]{mHost.group(1), "3306"} : null;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean testDatabaseConnection(String host, String port, String dbName, String user, String pass) {
        String testUrl = String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=true&serverTimezone=UTC",
                host, port, dbName
        );
        try (Connection ignored = DriverManager.getConnection(testUrl, user, pass)) {
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    public Object login(String username, String password, String selectedRole) throws SQLException {
        Object authenticatedUser = null;

        boolean isLocked = false;
        int remainingMinutes = 0;

        if (selectedRole.equalsIgnoreCase("Patient")) {
            isLocked = patientDAO.isAccountLocked(username);
            if (isLocked) {
                remainingMinutes = patientDAO.getRemainingLockoutMinutes(username);
                return new Object[]{"ACCOUNT_LOCKED", remainingMinutes};
            }
        } else {
            isLocked = staffDAO.isAccountLocked(username);
            if (isLocked) {
                remainingMinutes = staffDAO.getRemainingLockoutMinutes(username);
                return new Object[]{"ACCOUNT_LOCKED", remainingMinutes};
            }
        }

        if (selectedRole.equalsIgnoreCase("Patient")) {
            authenticatedUser = patientDAO.login(username, password);
        } else {
            authenticatedUser = staffDAO.login(username, password, selectedRole); 
        }

        if (authenticatedUser != null) {
            if (selectedRole.equalsIgnoreCase("Patient")) {
                patientDAO.resetFailedLoginAttempts(username);
            } else {
                staffDAO.resetFailedLoginAttempts(username);
            }

            String logMessage = "Success: " + selectedRole + " logged into the system.";
            LogService.logSystemEvent("INFO", "AuthService", logMessage);

            if (selectedRole.equalsIgnoreCase("Patient") && authenticatedUser instanceof Patient) {
                Patient patient = (Patient) authenticatedUser;
                if (patientDAO.needsPasswordReset(patient.getPatientId())) {
                    return new Object[]{"RESET_REQUIRED", patient};
                }
            } else if (authenticatedUser instanceof Object[]) {
                Object[] data = (Object[]) authenticatedUser;
                int staffId = (int) data[0];
                if (staffDAO.needsPasswordReset(staffId)) {
                    return new Object[]{"RESET_REQUIRED", authenticatedUser};
                }
            }

            return authenticatedUser;
        } else {
            if (selectedRole.equalsIgnoreCase("Patient")) {
                patientDAO.recordFailedLoginAttempt(username);
                if (patientDAO.isAccountLocked(username)) {
                    int remaining = patientDAO.getRemainingLockoutMinutes(username);
                    LogService.logSystemEvent("WARNING", "AuthService", "Account locked after multiple failed attempts");
                    return new Object[]{"ACCOUNT_LOCKED", remaining};
                }
            } else {
                staffDAO.recordFailedLoginAttempt(username);
                if (staffDAO.isAccountLocked(username)) {
                    int remaining = staffDAO.getRemainingLockoutMinutes(username);
                    LogService.logSystemEvent("WARNING", "AuthService", "Account locked after multiple failed attempts");
                    return new Object[]{"ACCOUNT_LOCKED", remaining};
                }
            }

            LogService.logSystemEvent("WARNING", "AuthService", "Failed login attempt for role: " + selectedRole);
            return null;
        }
    }

        public boolean registerNewPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String pass) throws java.sql.SQLException {
        // ==========================================================
        // SECURITY FIX: Cross-table username uniqueness check
        // ==========================================================
        
        // Check if username already exists in patients table
        if (patientDAO.isUsernameTakenInPatients(user)) {
            throw new IllegalArgumentException("Username already taken by another patient. Please choose another username.");
        }
        
        // Check if username already exists in staff table
        if (staffDAO.isUsernameTakenInStaff(user)) {
            throw new IllegalArgumentException("Username already exists as a staff account. Please choose another username.");
        }
        
        List<String> passwordErrors = PasswordValidator.validatePassword(pass);
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet requirements: " + String.join(", ", passwordErrors));
        }

        boolean success = patientDAO.registerPatient(fName, mName, lName, dob, age, addr, phone, email, user, pass);

        if (success) {
            LogService.logSystemEvent("INFO", "AuthService", "New patient registered: " + maskUsername(user));
            String fullName = fName + " " + lName;

            // SECURITY FIX: Password is no longer passed to the email method.
            // Sending plaintext passwords via email is a security risk — emails can be
            // intercepted, stored in mail server logs, or forwarded unintentionally.
            // The patient already knows their password (they just typed it).
            // The welcome email now only confirms their username for reference.
            EmailUtil.sendWelcomeEmailAsync(fullName, email, user);
        }

        return success;
    }

    // ==========================================================
    // PASSWORD RESET METHODS (USERNAME-BASED)
    // ==========================================================

    /**
     * Request password reset by username - sends code to associated email
     * @return masked email for display, null if username not found
     */
    public String requestPasswordResetByUsername(String username) throws SQLException {
        // Find user by username
        String email = passwordResetDAO.getEmailByUsername(username);
        String userType = null;
        
        if (email != null) {
            userType = passwordResetDAO.getUserTypeByUsername(username);
        }
        
        if (email == null || userType == null) {
            return null;
        }
        
        // Generate 6-digit code
        String code = PasswordResetDAO.generateCode();
        
        // Save to database
        boolean saved = passwordResetDAO.saveResetCode(email, code, userType, username);
        
        if (saved) {
            // Send email with code
            EmailUtil.sendPasswordResetCode(email, code);
            return maskEmail(email);
        }
        
        return null;
    }

    /**
     * Verify reset code
     */
    public boolean verifyResetCode(String code) throws SQLException {
        String email = passwordResetDAO.verifyCode(code);
        return email != null;
    }

    /**
     * Reset password by username (using the reset code)
     */
    public boolean resetPasswordByUsername(String username, String code, String newPassword) throws SQLException {
        // Verify the code first
        String email = passwordResetDAO.verifyCode(code);
        if (email == null) {
            return false;
        }
        
        // Verify that the username matches the email
        String userEmail = passwordResetDAO.getEmailByUsername(username);
        if (userEmail == null || !userEmail.equals(email)) {
            return false;
        }
        
        // Validate new password complexity
        List<String> passwordErrors = PasswordValidator.validatePassword(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet requirements: " + String.join(", ", passwordErrors));
        }
        
        // Hash the new password
        String hashedPassword = com.dentalclinic.util.PasswordUtil.hashPassword(newPassword);
        
        // Get user type and update password
        String userType = passwordResetDAO.getUserTypeByUsername(username);
        boolean success = false;
        
        if ("patient".equals(userType)) {
            success = passwordResetDAO.updatePatientPasswordByUsername(username, hashedPassword);
        } else if ("staff".equals(userType)) {
            success = passwordResetDAO.updateStaffPasswordByUsername(username, hashedPassword);
        }
        
        if (success) {
            // Mark code as used
            passwordResetDAO.markCodeAsUsed(code);
            LogService.logSystemEvent("INFO", "AuthService", "Password reset completed for username: " + maskUsername(username));
        }
        
        return success;
    }

    public boolean resetForcedPasswordForPatient(int patientId, String newPassword) throws SQLException {
        List<String> passwordErrors = PasswordValidator.validatePassword(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet requirements: " + String.join(", ", passwordErrors));
        }
        return patientDAO.updatePasswordAndClearReset(patientId, PasswordUtil.hashPassword(newPassword));
    }

    public boolean resetForcedPasswordForStaff(int staffId, String newPassword) throws SQLException {
        List<String> passwordErrors = PasswordValidator.validatePassword(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet requirements: " + String.join(", ", passwordErrors));
        }
        return staffDAO.updatePasswordAndClearReset(staffId, PasswordUtil.hashPassword(newPassword));
    }

    /**
     * Mask email for logging (privacy)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "unknown";
        String[] parts = email.split("@");
        String localPart = parts[0];
        if (localPart.length() <= 3) {
            return "***@" + parts[1];
        }
        return localPart.substring(0, 3) + "***@" + parts[1];
    }

    /**
     * Mask username for logging (privacy)
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 3) return "***";
        return username.substring(0, 2) + "***";
    }
}
