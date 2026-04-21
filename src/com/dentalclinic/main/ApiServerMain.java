package com.dentalclinic.main;

import com.dentalclinic.api.ApiServer;

public class ApiServerMain {

    public static void main(String[] args) {
        int port = resolvePort(args);
        try {
            ApiServer server = new ApiServer(port);
            server.start();
            System.out.println("Dental Clinic API server started on port " + port);
            System.out.println("Endpoints: GET /health, POST /auth/login");
        } catch (Exception ex) {
            System.err.println("Failed to start API server: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static int resolvePort(String[] args) {
        if (args != null && args.length > 0) {
            return parsePort(args[0], 8080);
        }

        String envPort = System.getenv("API_PORT");
        return parsePort(envPort, 8080);
    }

    private static int parsePort(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1 || parsed > 65535) {
                return fallback;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
