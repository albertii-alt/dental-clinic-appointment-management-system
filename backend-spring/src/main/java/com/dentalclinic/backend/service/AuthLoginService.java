package com.dentalclinic.backend.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.dentalclinic.backend.dto.LoginRequestDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthLoginService {

    private final JdbcTemplate jdbcTemplate;

    public AuthLoginService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isDatabaseAvailable() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1;
        } catch (Exception ex) {
            return false;
        }
    }

    public AuthLoginResult login(LoginRequestDto requestDto) {
        String selectedRole = requestDto.getSelectedRole();
        if (selectedRole != null && selectedRole.equalsIgnoreCase("Patient")) {
            return loginPatient(requestDto);
        }
        return loginStaff(requestDto);
    }

    private AuthLoginResult loginPatient(LoginRequestDto requestDto) {
        String username = requestDto.getUsername();

        if (isPatientAccountLocked(username)) {
            return new AuthLoginResult(423, mapOf(
                    "status", "account_locked",
                    "remainingMinutes", getPatientRemainingLockoutMinutes(username)
            ));
        }

        String query = "SELECT patient_id, first_name, middle_name, last_name, birth_date, age, address, " +
                "contact_number, email, username, password, force_password_reset " +
                "FROM patients WHERE username = ?";

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(query, username);
        } catch (EmptyResultDataAccessException ex) {
            recordFailedPatientLogin(username);
            return invalidCredentials();
        }

        if (!verifyPassword(requestDto.getPassword(), (String) row.get("password"))) {
            recordFailedPatientLogin(username);
            if (isPatientAccountLocked(username)) {
                return new AuthLoginResult(423, mapOf(
                        "status", "account_locked",
                        "remainingMinutes", getPatientRemainingLockoutMinutes(username)
                ));
            }
            return invalidCredentials();
        }

        resetPatientFailedAttempts(username);

        Integer patientId = toInt(row.get("patient_id"), -1);
        if (toInt(row.get("force_password_reset"), 0) == 1) {
            return new AuthLoginResult(403, mapOf(
                    "status", "reset_required",
                    "userId", patientId,
                    "roleName", "PATIENT"
            ));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("loginStatus", "SUCCESS_PATIENT");
        body.put("userId", patientId);
        body.put("roleName", "PATIENT");
        body.put("fullName", joinName((String) row.get("first_name"), (String) row.get("last_name")));
        body.put("email", row.get("email"));
        body.put("superAdmin", false);
        body.put("permissions", Collections.emptyList());

        body.put("firstName", row.get("first_name"));
        body.put("middleName", row.get("middle_name"));
        body.put("lastName", row.get("last_name"));
        body.put("birthDate", dateToString(row.get("birth_date")));
        body.put("age", toInt(row.get("age"), 0));
        body.put("address", row.get("address"));
        body.put("contactNumber", row.get("contact_number"));
        body.put("username", row.get("username"));

        return new AuthLoginResult(200, body);
    }

    private AuthLoginResult loginStaff(LoginRequestDto requestDto) {
        String username = requestDto.getUsername();

        if (isStaffAccountLocked(username)) {
            return new AuthLoginResult(423, mapOf(
                    "status", "account_locked",
                    "remainingMinutes", getStaffRemainingLockoutMinutes(username)
            ));
        }

        String query = "SELECT s.staff_id, s.role_id, r.role_name, s.is_super_admin, s.full_name, s.email, s.password, s.force_password_reset " +
                "FROM staff s JOIN roles r ON s.role_id = r.role_id " +
                "WHERE s.username = ? AND UPPER(r.role_name) = UPPER(?) AND s.is_active = 1";

        Map<String, Object> row;
        try {
            row = jdbcTemplate.queryForMap(query, username, requestDto.getSelectedRole());
        } catch (EmptyResultDataAccessException ex) {
            recordFailedStaffLogin(username);
            return invalidCredentials();
        }

        if (!verifyPassword(requestDto.getPassword(), (String) row.get("password"))) {
            recordFailedStaffLogin(username);
            if (isStaffAccountLocked(username)) {
                return new AuthLoginResult(423, mapOf(
                        "status", "account_locked",
                        "remainingMinutes", getStaffRemainingLockoutMinutes(username)
                ));
            }
            return invalidCredentials();
        }

        resetStaffFailedAttempts(username);

        Integer staffId = toInt(row.get("staff_id"), -1);
        String roleName = normalize((String) row.get("role_name")).toUpperCase();
        if (toInt(row.get("force_password_reset"), 0) == 1) {
            return new AuthLoginResult(403, mapOf(
                    "status", "reset_required",
                    "userId", staffId,
                    "roleName", roleName
            ));
        }

        Integer roleId = toInt(row.get("role_id"), -1);
        List<String> permissions = roleId > 0 ? loadPermissions(roleId) : Collections.emptyList();

        return new AuthLoginResult(200, mapOf(
                "status", "success",
                "loginStatus", "SUCCESS_STAFF",
                "userId", staffId,
                "roleName", roleName,
                "fullName", row.get("full_name"),
                "email", row.get("email"),
                "superAdmin", toInt(row.get("is_super_admin"), 0) == 1,
                "permissions", permissions
        ));
    }

    private List<String> loadPermissions(int roleId) {
        String query = "SELECT p.permission_name FROM permissions p " +
                "JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                "WHERE rp.role_id = ?";
        return jdbcTemplate.query(query, (rs, rowNum) -> rs.getString("permission_name"), roleId);
    }

    private AuthLoginResult invalidCredentials() {
        return new AuthLoginResult(401, mapOf("status", "invalid_credentials"));
    }

    private void recordFailedPatientLogin(String username) {
        String query = "UPDATE patients SET failed_login_attempts = failed_login_attempts + 1, " +
                "account_locked = CASE WHEN failed_login_attempts + 1 >= 5 THEN 1 ELSE 0 END, " +
                "lockout_time = CASE WHEN failed_login_attempts + 1 >= 5 THEN NOW() ELSE lockout_time END " +
                "WHERE username = ?";
        jdbcTemplate.update(query, username);
    }

    private void recordFailedStaffLogin(String username) {
        String query = "UPDATE staff SET failed_login_attempts = failed_login_attempts + 1, " +
                "account_locked = CASE WHEN failed_login_attempts + 1 >= 5 THEN 1 ELSE 0 END, " +
                "lockout_time = CASE WHEN failed_login_attempts + 1 >= 5 THEN NOW() ELSE lockout_time END " +
                "WHERE username = ? AND is_active = 1";
        jdbcTemplate.update(query, username);
    }

    private boolean isPatientAccountLocked(String username) {
        String query = "SELECT account_locked, lockout_time, TIMESTAMPDIFF(MINUTE, lockout_time, NOW()) AS minutes_elapsed " +
                "FROM patients WHERE username = ?";

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(query, username);
            boolean accountLocked = toInt(row.get("account_locked"), 0) == 1;
            Object lockoutTime = row.get("lockout_time");
            int minutesElapsed = toInt(row.get("minutes_elapsed"), 0);

            if (accountLocked && lockoutTime != null && minutesElapsed >= 30) {
                resetPatientFailedAttempts(username);
                return false;
            }
            return accountLocked;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private boolean isStaffAccountLocked(String username) {
        String query = "SELECT account_locked, lockout_time, TIMESTAMPDIFF(MINUTE, lockout_time, NOW()) AS minutes_elapsed " +
                "FROM staff WHERE username = ?";

        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(query, username);
            boolean accountLocked = toInt(row.get("account_locked"), 0) == 1;
            Object lockoutTime = row.get("lockout_time");
            int minutesElapsed = toInt(row.get("minutes_elapsed"), 0);

            if (accountLocked && lockoutTime != null && minutesElapsed >= 30) {
                resetStaffFailedAttempts(username);
                return false;
            }
            return accountLocked;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private int getPatientRemainingLockoutMinutes(String username) {
        String query = "SELECT TIMESTAMPDIFF(MINUTE, lockout_time, NOW()) AS minutes_elapsed " +
                "FROM patients WHERE username = ? AND account_locked = 1";
        try {
            Integer elapsed = jdbcTemplate.queryForObject(query, Integer.class, username);
            if (elapsed == null) {
                return 0;
            }
            return Math.max(0, 30 - elapsed);
        } catch (EmptyResultDataAccessException ex) {
            return 0;
        }
    }

    private int getStaffRemainingLockoutMinutes(String username) {
        String query = "SELECT TIMESTAMPDIFF(MINUTE, lockout_time, NOW()) AS minutes_elapsed " +
                "FROM staff WHERE username = ? AND account_locked = 1";
        try {
            Integer elapsed = jdbcTemplate.queryForObject(query, Integer.class, username);
            if (elapsed == null) {
                return 0;
            }
            return Math.max(0, 30 - elapsed);
        } catch (EmptyResultDataAccessException ex) {
            return 0;
        }
    }

    private void resetPatientFailedAttempts(String username) {
        jdbcTemplate.update("UPDATE patients SET failed_login_attempts = 0, account_locked = 0, lockout_time = NULL WHERE username = ?", username);
    }

    private void resetStaffFailedAttempts(String username) {
        jdbcTemplate.update("UPDATE staff SET failed_login_attempts = 0, account_locked = 0, lockout_time = NULL WHERE username = ?", username);
    }

    private boolean verifyPassword(String plainTextPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        return BCrypt.verifyer().verify(plainTextPassword.toCharArray(), hashedPassword).verified;
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private String joinName(String firstName, String lastName) {
        String first = normalize(firstName).trim();
        String last = normalize(lastName).trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? null : full;
    }

    private String dateToString(Object dateValue) {
        if (dateValue instanceof Date date) {
            return date.toString();
        }
        return dateValue == null ? null : String.valueOf(dateValue);
    }

    private Map<String, Object> mapOf(Object... items) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < items.length; i += 2) {
            map.put(String.valueOf(items[i]), items[i + 1]);
        }
        return map;
    }
}
