package com.dentalclinic.controller;

import com.dentalclinic.dto.auth.LoginRequest;
import com.dentalclinic.dto.auth.LoginResult;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AuthService;
import com.dentalclinic.service.RolesService;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AuthController {
    private final AuthService authService = new AuthService();
    private final RolesService rolesService = new RolesService();

    public boolean isDatabaseAvailable() {
        return authService.isDatabaseAvailable();
    }

    public boolean testDatabaseConnection(String host, String port, String dbName, String user, String pass) {
        return authService.testDatabaseConnection(host, port, dbName, user, pass);
    }

    public LoginResult login(LoginRequest request) throws SQLException {
        Object raw = authService.login(request.getUsername(), request.getPassword(), request.getSelectedRole());

        if (raw instanceof Object[] && "ACCOUNT_LOCKED".equals(((Object[]) raw)[0])) {
            return new LoginResult(
                    LoginResult.Status.ACCOUNT_LOCKED, -1, null, false, null, null,
                    null, (int) ((Object[]) raw)[1], Collections.emptyList()
            );
        }

        if (raw instanceof Object[] && "RESET_REQUIRED".equals(((Object[]) raw)[0])) {
            Object resetData = ((Object[]) raw)[1];
            if (resetData instanceof Patient) {
                Patient patient = (Patient) resetData;
                return new LoginResult(
                        LoginResult.Status.RESET_REQUIRED, patient.getPatientId(), "PATIENT", false,
                        patient.getFullName(), patient.getEmail(), patient, 0, Collections.emptyList()
                );
            }

            Object[] staffData = (Object[]) resetData;
            return new LoginResult(
                    LoginResult.Status.RESET_REQUIRED, (int) staffData[0], ((String) staffData[1]).toUpperCase(),
                    (boolean) staffData[2], (String) staffData[3], (String) staffData[4], null, 0, Collections.emptyList()
            );
        }

        if (raw instanceof Patient) {
            Patient patient = (Patient) raw;
            return new LoginResult(
                    LoginResult.Status.SUCCESS_PATIENT, patient.getPatientId(), "PATIENT", false,
                    patient.getFullName(), patient.getEmail(), patient, 0, Collections.emptyList()
            );
        }

        if (raw instanceof Object[]) {
            Object[] data = (Object[]) raw;
            String roleName = ((String) data[1]).toUpperCase();
            int roleId = rolesService.getRoleIdByName(roleName);
            List<String> permissions = roleId > 0 ? rolesService.getPermissionNamesForRole(roleId) : Collections.emptyList();
            return new LoginResult(
                    LoginResult.Status.SUCCESS_STAFF, (int) data[0], roleName, (boolean) data[2],
                    (String) data[3], (String) data[4], null, 0, permissions
            );
        }

        return new LoginResult(
                LoginResult.Status.INVALID_CREDENTIALS, -1, null, false, null, null,
                null, 0, Collections.emptyList()
        );
    }

    public boolean resetForcedPassword(int userId, String roleName, String newPassword) throws SQLException {
        if ("PATIENT".equalsIgnoreCase(roleName)) {
            return authService.resetForcedPasswordForPatient(userId, newPassword);
        }
        return authService.resetForcedPasswordForStaff(userId, newPassword);
    }

    public String requestPasswordResetByUsername(String username) throws SQLException {
        return authService.requestPasswordResetByUsername(username);
    }

    public boolean verifyResetCode(String code) throws SQLException {
        return authService.verifyResetCode(code);
    }

    public boolean resetPasswordByUsername(String username, String resetCode, String newPassword) throws SQLException {
        return authService.resetPasswordByUsername(username, resetCode, newPassword);
    }

    public boolean registerNewPatient(String fName, String mName, String lName, java.sql.Date dob, int age, String addr,
                                      String phone, String email, String user, String pass) throws SQLException {
        return authService.registerNewPatient(fName, mName, lName, dob, age, addr, phone, email, user, pass);
    }

    public boolean registerNewPatient(String fName, String mName, String lName, Date dob, int age, String addr,
                                      String phone, String email, String user, String pass) throws SQLException {
        java.sql.Date sqlDob = new java.sql.Date(dob.getTime());
        return authService.registerNewPatient(fName, mName, lName, sqlDob, age, addr, phone, email, user, pass);
    }
}
