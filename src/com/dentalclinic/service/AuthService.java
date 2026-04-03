package com.dentalclinic.service;

import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.dao.StaffDAO;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.EmailUtil;
import java.sql.SQLException;
import com.dentalclinic.util.PasswordValidator;
import java.util.List;
import java.sql.Timestamp;

public class AuthService {
    private PatientDAO patientDAO = new PatientDAO();
    private StaffDAO staffDAO = new StaffDAO();

    public Object login(String username, String password, String selectedRole) throws SQLException {
        Object authenticatedUser = null;

        // First check if account is locked (for both patient and staff)
        boolean isLocked = false;
        int remainingMinutes = 0;

        if (selectedRole.equalsIgnoreCase("Patient")) {
            isLocked = patientDAO.isAccountLocked(username);
            if (isLocked) {
                remainingMinutes = patientDAO.getRemainingLockoutMinutes(username);
                // Return a special object indicating account is locked
                return new Object[]{"ACCOUNT_LOCKED", remainingMinutes};
            }
        } else {
            isLocked = staffDAO.isAccountLocked(username);
            if (isLocked) {
                remainingMinutes = staffDAO.getRemainingLockoutMinutes(username);
                return new Object[]{"ACCOUNT_LOCKED", remainingMinutes};
            }
        }

        // 1. Attempt Authentication
        if (selectedRole.equalsIgnoreCase("Patient")) {
            authenticatedUser = patientDAO.login(username, password);
        } else {
            authenticatedUser = staffDAO.login(username, password, selectedRole); 
        }

        // 2. Handle authentication result with lockout tracking
        if (authenticatedUser != null) {
            // Successful login - reset failed attempts
            if (selectedRole.equalsIgnoreCase("Patient")) {
                patientDAO.resetFailedLoginAttempts(username);
            } else {
                staffDAO.resetFailedLoginAttempts(username);
            }

            // FIX: Remove username from successful login log
            String logMessage = "Success: " + selectedRole + " logged into the system.";
            LogService.logSystemEvent("INFO", "AuthService", logMessage);

            // Check for password reset requirement
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
            // Failed login - record the attempt
            if (selectedRole.equalsIgnoreCase("Patient")) {
                patientDAO.recordFailedLoginAttempt(username);
                // Check if now locked
                if (patientDAO.isAccountLocked(username)) {
                    int remaining = patientDAO.getRemainingLockoutMinutes(username);
                    // FIX: Remove username from lockout log
                    LogService.logSystemEvent("WARNING", "AuthService", "Account locked after multiple failed attempts");
                    return new Object[]{"ACCOUNT_LOCKED", remaining};
                }
            } else {
                staffDAO.recordFailedLoginAttempt(username);
                if (staffDAO.isAccountLocked(username)) {
                    int remaining = staffDAO.getRemainingLockoutMinutes(username);
                    // FIX: Remove username from lockout log
                    LogService.logSystemEvent("WARNING", "AuthService", "Account locked after multiple failed attempts");
                    return new Object[]{"ACCOUNT_LOCKED", remaining};
                }
            }

            // FIX: Already removed username from failed login log
            LogService.logSystemEvent("WARNING", "AuthService", "Failed login attempt for role: " + selectedRole);
            return null;
        }
    }

    public boolean registerNewPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String pass) throws java.sql.SQLException {
        // Validate password complexity
        List<String> passwordErrors = PasswordValidator.validatePassword(pass);
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet requirements: " + String.join(", ", passwordErrors));
        }

        boolean success = patientDAO.registerPatient(fName, mName, lName, dob, age, addr, phone, email, user, pass);

        if (success) {
            // FIX: Remove username from registration log - just log the action without PII
            LogService.logSystemEvent("INFO", "AuthService", "New patient registered");
            
                 // Send welcome email
            String fullName = fName + " " + lName;
            EmailUtil.sendWelcomeEmailAsync(fullName, email, user, pass);
        }

        return success;
    }
}