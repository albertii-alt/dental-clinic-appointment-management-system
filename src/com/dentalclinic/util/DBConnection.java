package com.dentalclinic.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {
    
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    
    // Database configuration
    private static String URL;
    private static String USER;
    private static String PASSWORD;
    private static boolean configLoaded = false;
    private static String configError = null;
    private static HikariDataSource dataSource;
    
    // SECURITY: Load configuration on class initialization
    static {
        loadConfigFromFile();
        
        if (!configLoaded) {
            loadConfigFromEnv();
        }
        
        if (!configLoaded) {
            LOGGER.warning("Database configuration NOT loaded: " + configError);
        } else {
            initializeDataSource();
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            closeDataSource();
        }));
    }
    
    // ==========================================================
    // CONFIGURATION LOADING
    // ==========================================================
    
    private static void loadConfigFromFile() {
        String userHome = System.getProperty("user.home");
        String configPath = userHome + File.separator + ".dental_clinic" + File.separator + "db.properties";
        
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            configError = "Config file not found at: " + configPath;
            return;
        }
        
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(configFile)) {
            props.load(input);
            
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
            
            if (URL == null || URL.trim().isEmpty()) {
                configError = "db.url not specified in config file";
                return;
            }
            if (USER == null || USER.trim().isEmpty()) {
                configError = "db.user not specified in config file";
                return;
            }
            
            configLoaded = true;
            LOGGER.info("Database config loaded from: " + configPath);
            
        } catch (IOException e) {
            configError = "Failed to read config file: " + e.getMessage();
            LOGGER.log(Level.WARNING, configError, e);
        }
    }
    
    private static void loadConfigFromEnv() {
        URL = System.getenv("DB_URL");
        USER = System.getenv("DB_USER");
        PASSWORD = System.getenv("DB_PASSWORD");
        
        if (URL != null && !URL.trim().isEmpty() && 
            USER != null && !USER.trim().isEmpty()) {
            configLoaded = true;
            LOGGER.info("Database config loaded from environment variables");
        } else {
            configError = "Environment variables not set (DB_URL, DB_USER)";
        }
    }
    
    // ==========================================================
    // CONNECTION MANAGEMENT
    // ==========================================================

    private static void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(buildOptimizedJdbcUrl(URL));
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setMaximumPoolSize(10);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(300000);
            config.setInitializationFailTimeout(-1); // Don't fail on startup - connect lazily
            config.setPoolName("DentalClinicHikariPool");

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");

            dataSource = new HikariDataSource(config);
            LOGGER.info("Database configuration loaded successfully (HikariCP enabled)");
        } catch (RuntimeException ex) {
            configLoaded = false;
            configError = "Failed to initialize HikariCP: " + ex.getMessage();
            LOGGER.log(Level.SEVERE, configError, ex);
        }
    }

    private static String buildOptimizedJdbcUrl(String baseUrl) {
        StringBuilder url = new StringBuilder(baseUrl == null ? "" : baseUrl.trim());
        appendJdbcParam(url, "cachePrepStmts", "true");
        appendJdbcParam(url, "prepStmtCacheSize", "250");
        appendJdbcParam(url, "rewriteBatchedStatements", "true");
        return url.toString();
    }

    private static void appendJdbcParam(StringBuilder url, String key, String value) {
        String lower = url.toString().toLowerCase();
        String prefix = key.toLowerCase() + "=";
        if (lower.contains(prefix)) {
            return;
        }

        if (url.indexOf("?") >= 0) {
            if (url.charAt(url.length() - 1) != '?' && url.charAt(url.length() - 1) != '&') {
                url.append('&');
            }
        } else {
            url.append('?');
        }
        url.append(key).append('=').append(value);
    }

    private static void closeDataSource() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error while closing HikariCP data source", ex);
            }
        }
    }
    
    public static Connection getConnection() throws SQLException {
        if (!configLoaded || dataSource == null) {
            throw new SQLException("Database not configured. Please run the setup wizard or check config file.\n" + 
                                   "Error: " + configError);
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            String safeMessage = getSafeErrorMessage(e);
            LOGGER.log(Level.SEVERE, "Database connection failed: " + safeMessage);
            throw new SQLException("Unable to connect to the database. Please check:\n" +
                                   "1. MySQL server is running\n" +
                                   "2. Database credentials are correct\n" +
                                   "3. Database 'dental_clinic_db' exists");
        }
    }
    
    private static String getSafeErrorMessage(SQLException e) {
        String message = e.getMessage();
        if (message == null) return "Unknown database error";
        
        message = message.replaceAll("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b", "[IP]");
        message = message.replaceAll("password[^,]*", "[PASSWORD]");
        message = message.replaceAll("passwd[^,]*", "[PASSWORD]");
        message = message.replaceAll("user '[^']*'", "user '[USER]'");
        message = message.replaceAll("username '[^']*'", "username '[USER]'");
        
        return message;
    }
    
    public static boolean isConfigLoaded() {
        return configLoaded && dataSource != null;
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean valid = conn != null && !conn.isClosed();
            if (valid) {
                LOGGER.info("Database connection test successful");
            }
            return valid;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    public static String getConnectionStats() {
        if (dataSource == null) {
            return String.format("Active: %d/%d, Idle: %d, Configured: %s",
                0, 0, 0, configLoaded ? "Yes" : "No");
        }

        return String.format("Active: %d/%d, Idle: %d, Configured: %s",
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getMaximumPoolSize(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            configLoaded ? "Yes" : "No");
    }
    
    public static void logActiveConnections() {
        if (dataSource == null) {
            System.err.println("=== POOL NOT INITIALIZED ===");
            return;
        }

        System.err.println("=== ACTIVE CONNECTIONS: " +
                dataSource.getHikariPoolMXBean().getActiveConnections() + "/" +
                dataSource.getMaximumPoolSize() + " ===");
    }
    
    public static int getActiveConnectionCount() {
        if (dataSource == null) {
            return 0;
        }
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }
}