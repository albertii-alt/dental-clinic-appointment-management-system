package com.dentalclinic.api;

import com.dentalclinic.controller.AuthController;
import com.dentalclinic.dto.auth.LoginRequest;
import com.dentalclinic.dto.auth.LoginResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;

public class ApiServer {
    private final HttpServer server;
    private final AuthController authController;

    public ApiServer(int port) throws IOException {
        this.authController = new AuthController();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/health", new HealthHandler());
        this.server.createContext("/auth/login", new LoginHandler());
        this.server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        this.server.start();
    }

    public void stop(int delaySeconds) {
        this.server.stop(delaySeconds);
    }

    private final class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"status\":\"error\",\"message\":\"Method not allowed\"}");
                return;
            }

            boolean dbReady = authController.isDatabaseAvailable();
            if (dbReady) {
                send(exchange, 200, "{\"status\":\"ok\",\"message\":\"service and database are ready\"}");
            } else {
                send(exchange, 503, "{\"status\":\"error\",\"message\":\"database unavailable\"}");
            }
        }
    }

    private final class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if ("OPTIONS".equalsIgnoreCase(method)) {
                addCommonHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                send(exchange, 405, "{\"status\":\"error\",\"message\":\"Method not allowed\"}");
                return;
            }

            String body = readBody(exchange.getRequestBody());
            Map<String, String> values = MiniJson.parseLoginBody(body);

            String username = trim(values.get("username"));
            String password = values.get("password");
            String selectedRole = trim(values.get("selectedRole"));

            if (username == null || username.isEmpty() || password == null || password.isEmpty()
                    || selectedRole == null || selectedRole.isEmpty()) {
                send(exchange, 400, "{\"status\":\"error\",\"message\":\"username, password, and selectedRole are required\"}");
                return;
            }

            try {
                LoginResult result = authController.login(new LoginRequest(username, password, selectedRole));
                sendLoginResult(exchange, result);
            } catch (SQLException ex) {
                send(exchange, 500, "{\"status\":\"error\",\"message\":\"login failed due to server error\"}");
            }
        }

        private void sendLoginResult(HttpExchange exchange, LoginResult result) throws IOException {
            LoginResult.Status status = result.getStatus();
            switch (status) {
                case SUCCESS_PATIENT:
                case SUCCESS_STAFF:
                    StringBuilder payload = new StringBuilder()
                        .append("{")
                        .append("\"status\":\"success\",")
                        .append("\"loginStatus\":").append(MiniJson.string(status.name())).append(",")
                        .append("\"userId\":").append(MiniJson.number(result.getUserId())).append(",")
                        .append("\"roleName\":").append(MiniJson.string(result.getRoleName())).append(",")
                        .append("\"fullName\":").append(MiniJson.string(result.getFullName())).append(",")
                        .append("\"email\":").append(MiniJson.string(result.getEmail())).append(",")
                        .append("\"superAdmin\":").append(MiniJson.bool(result.isSuperAdmin())).append(",")
                        .append("\"permissions\":").append(MiniJson.stringArray(result.getPermissions()));

                    if (status == LoginResult.Status.SUCCESS_PATIENT && result.getPatient() != null) {
                    payload.append(",\"firstName\":").append(MiniJson.string(result.getPatient().getFirstName()))
                        .append(",\"middleName\":").append(MiniJson.string(result.getPatient().getMiddleName()))
                        .append(",\"lastName\":").append(MiniJson.string(result.getPatient().getLastName()))
                        .append(",\"birthDate\":").append(MiniJson.string(result.getPatient().getBirthDate() != null ? result.getPatient().getBirthDate().toString() : null))
                        .append(",\"age\":").append(MiniJson.number(result.getPatient().getAge()))
                        .append(",\"address\":").append(MiniJson.string(result.getPatient().getAddress()))
                        .append(",\"contactNumber\":").append(MiniJson.string(result.getPatient().getContactNumber()))
                        .append(",\"username\":").append(MiniJson.string(result.getPatient().getUsername()));
                    }

                    payload.append("}");
                    send(exchange, 200, payload.toString());
                    break;
                case ACCOUNT_LOCKED:
                    send(exchange, 423,
                            "{"
                                    + "\"status\":\"account_locked\","
                                    + "\"remainingMinutes\":" + MiniJson.number(result.getRemainingMinutes())
                                    + "}");
                    break;
                case RESET_REQUIRED:
                    send(exchange, 403,
                            "{"
                                    + "\"status\":\"reset_required\","
                                    + "\"userId\":" + MiniJson.number(result.getUserId()) + ","
                                    + "\"roleName\":" + MiniJson.string(result.getRoleName())
                                    + "}");
                    break;
                default:
                    send(exchange, 401, "{\"status\":\"invalid_credentials\"}");
                    break;
            }
        }
    }

    private void send(HttpExchange exchange, int statusCode, String body) throws IOException {
        addCommonHeaders(exchange);
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    private void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }

    private String readBody(InputStream inputStream) throws IOException {
        byte[] data = inputStream.readAllBytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
