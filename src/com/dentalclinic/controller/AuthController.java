package com.dentalclinic.controller;

import com.dentalclinic.dto.auth.LoginRequest;
import com.dentalclinic.dto.auth.LoginResult;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AuthService;
import com.dentalclinic.service.RolesService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthController {
    private final AuthService authService = new AuthService();
    private final RolesService rolesService = new RolesService();
    private final boolean useApiAuth;
    private final HttpClient httpClient;
    private final String apiBaseUrl;

    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"");
    private static final Pattern NUMBER_FIELD_PATTERN = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(-?\\\\d+)");
    private static final Pattern BOOL_FIELD_PATTERN = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARRAY_FIELD_PATTERN = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);

    public AuthController() {
        this(false);
    }

    public AuthController(boolean useApiAuth) {
        this.useApiAuth = useApiAuth;
        this.httpClient = HttpClient.newHttpClient();

        String envBaseUrl = System.getenv("API_BASE_URL");
        if (envBaseUrl == null || envBaseUrl.trim().isEmpty()) {
            // Default to Render backend if not set
            this.apiBaseUrl = "https://dental-clinic-backend-nf8y.onrender.com";
        } else {
            this.apiBaseUrl = envBaseUrl.trim().replaceAll("/+$$", "");
        }
    }

    public boolean isDatabaseAvailable() {
        if (useApiAuth) {
            return isApiAvailable();
        }
        return authService.isDatabaseAvailable();
    }

    public boolean testDatabaseConnection(String host, String port, String dbName, String user, String pass) {
        return authService.testDatabaseConnection(host, port, dbName, user, pass);
    }

    public LoginResult login(LoginRequest request) throws SQLException {
        if (useApiAuth) {
            return loginViaApi(request);
        }

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

    private boolean isApiAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/health"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    private LoginResult loginViaApi(LoginRequest request) throws SQLException {
        try {
            String payload = "{"
                    + "\"username\":" + jsonString(request.getUsername()) + ","
                    + "\"password\":" + jsonString(request.getPassword()) + ","
                    + "\"selectedRole\":" + jsonString(request.getSelectedRole())
                    + "}";

            HttpRequest apiRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(apiRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return mapApiLoginResponse(response.statusCode(), response.body());
        } catch (IOException ex) {
            throw new SQLException("Unable to reach authentication API", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SQLException("Authentication request was interrupted", ex);
        }
    }

    private LoginResult mapApiLoginResponse(int statusCode, String body) throws SQLException {
        String loginStatus = extractString(body, "loginStatus");

        if (statusCode == 200 && "SUCCESS_STAFF".equals(loginStatus)) {
            return new LoginResult(
                    LoginResult.Status.SUCCESS_STAFF,
                    extractInt(body, "userId", -1),
                    extractString(body, "roleName"),
                    extractBoolean(body, "superAdmin", false),
                    extractString(body, "fullName"),
                    extractString(body, "email"),
                    null,
                    0,
                    extractStringArray(body, "permissions")
            );
        }

        if (statusCode == 200 && "SUCCESS_PATIENT".equals(loginStatus)) {
            Patient patient = new Patient(
                    extractInt(body, "userId", -1),
                    normalize(extractString(body, "firstName")),
                    normalize(extractString(body, "middleName")),
                    normalize(extractString(body, "lastName")),
                    parseDate(extractString(body, "birthDate")),
                    extractInt(body, "age", 0),
                    normalize(extractString(body, "address")),
                    normalize(extractString(body, "contactNumber")),
                    normalize(extractString(body, "email")),
                    normalize(extractString(body, "username"))
            );

            return new LoginResult(
                    LoginResult.Status.SUCCESS_PATIENT,
                    patient.getPatientId(),
                    "PATIENT",
                    false,
                    patient.getFullName(),
                    patient.getEmail(),
                    patient,
                    0,
                    Collections.emptyList()
            );
        }

        if (statusCode == 423) {
            return new LoginResult(
                    LoginResult.Status.ACCOUNT_LOCKED,
                    -1,
                    null,
                    false,
                    null,
                    null,
                    null,
                    extractInt(body, "remainingMinutes", 0),
                    Collections.emptyList()
            );
        }

        if (statusCode == 403) {
            return new LoginResult(
                    LoginResult.Status.RESET_REQUIRED,
                    extractInt(body, "userId", -1),
                    extractString(body, "roleName"),
                    false,
                    null,
                    null,
                    null,
                    0,
                    Collections.emptyList()
            );
        }

        if (statusCode == 401) {
            return new LoginResult(
                    LoginResult.Status.INVALID_CREDENTIALS,
                    -1,
                    null,
                    false,
                    null,
                    null,
                    null,
                    0,
                    Collections.emptyList()
            );
        }

        throw new SQLException("Authentication API returned unexpected response: " + statusCode);
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String extractString(String json, String field) {
        Matcher matcher = STRING_FIELD_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                return unescape(matcher.group(2));
            }
        }
        return null;
    }

    private int extractInt(String json, String field, int fallback) {
        Matcher matcher = NUMBER_FIELD_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                try {
                    return Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private boolean extractBoolean(String json, String field, boolean fallback) {
        Matcher matcher = BOOL_FIELD_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                return Boolean.parseBoolean(matcher.group(2));
            }
        }
        return fallback;
    }

    private List<String> extractStringArray(String json, String field) {
        Matcher matcher = ARRAY_FIELD_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                String content = matcher.group(2);
                Matcher valueMatcher = Pattern.compile("\\\"((?:\\\\.|[^\\\\\\\"])*)\\\"").matcher(content);
                java.util.ArrayList<String> out = new java.util.ArrayList<>();
                while (valueMatcher.find()) {
                    out.add(unescape(valueMatcher.group(1)));
                }
                return out;
            }
        }
        return Collections.emptyList();
    }

    private Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new Date();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        dateFormat.setLenient(false);
        try {
            return dateFormat.parse(value.trim());
        } catch (ParseException ex) {
            return new Date();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    private String unescape(String value) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '\"':
                        out.append('"');
                        break;
                    default:
                        out.append(c);
                        break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }

        if (escaping) {
            out.append('\\');
        }

        return out.toString();
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
