package com.dentalclinic.service;

import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.dao.StaffDAO;
import com.dentalclinic.model.Patient;
import java.sql.SQLException;

public class AuthService {
    private PatientDAO patientDAO = new PatientDAO();
    private StaffDAO staffDAO = new StaffDAO();

    public Object login(String username, String password, String selectedRole) throws SQLException {
        Object authenticatedUser = null;

        // 1. Attempt Authentication
        if (selectedRole.equalsIgnoreCase("Patient")) {
            authenticatedUser = patientDAO.login(username, password);
        } else {
            authenticatedUser = staffDAO.login(username, password, selectedRole); 
        }

        // 2. THE SUCCESS TEST: If user is found, log it to System Logs
        if (authenticatedUser != null) {
            String logMessage = "Success: " + selectedRole + " [" + username + "] logged into the system.";
            
            // Call our static logging utility
            LogService.logSystemEvent("INFO", "AuthService", logMessage);
        } else {
            // Optional: Log failed attempts as WARNINGS
            LogService.logSystemEvent("WARNING", "AuthService", "Failed login attempt for username: " + username);
        }

        return authenticatedUser;
    }

    public boolean registerNewPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr, String phone, String email, String user, String pass) throws java.sql.SQLException {
        boolean success = patientDAO.registerPatient(fName, mName, lName, dob, age, addr, phone, email, user, pass);
        
        // Log successful registration
        if (success) {
            LogService.logSystemEvent("INFO", "AuthService", "New patient registered: " + user);
        }
        
        return success;
    }
}