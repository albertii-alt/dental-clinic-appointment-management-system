package com.dentalclinic.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.SQLClientInfoException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.Properties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    // Connection tracking for debugging
    private static int activeConnections = 0;
    private static int totalConnectionsCreated = 0;
    private static Map<Connection, StackTraceElement[]> connectionTraces = new ConcurrentHashMap<>();
    
    // Connection limits
    private static final int MAX_CONNECTIONS = 200;
    private static final long CONNECTION_TIMEOUT_MS = 30000;
    
    // SECURITY: Load configuration on class initialization
    static {
        loadConfigFromFile();
        
        if (!configLoaded) {
            loadConfigFromEnv();
        }
        
        if (configLoaded) {
            LOGGER.info("Database configuration loaded successfully");
        } else {
            LOGGER.warning("Database configuration NOT loaded: " + configError);
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown - Final connection stats: " + getConnectionStats());
            if (activeConnections > 0) {
                LOGGER.warning("WARNING: " + activeConnections + " connections still active at shutdown!");
            }
        }));
    }
    
    // ==========================================================
    // CONNECTION WRAPPER CLASS
    // ==========================================================
    
    /**
     * Wrapper class that tracks connection closure
     */
    private static class TrackedConnection implements Connection {
        private final Connection delegate;
        private final StackTraceElement[] creationTrace;
        
        public TrackedConnection(Connection delegate) {
            this.delegate = delegate;
            this.creationTrace = Thread.currentThread().getStackTrace();
        }
        
        @Override
        public void close() throws SQLException {
            try {
                delegate.close();
            } finally {
                // Call our release method after closing
                releaseConnection(this, creationTrace);
            }
        }
        
        // Delegate all other methods to the real connection
        @Override public Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public boolean isClosed() throws SQLException { return delegate.isClosed(); }
        @Override public DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { 
            return delegate.createStatement(resultSetType, resultSetConcurrency); 
        }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { 
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency); 
        }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { 
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency); 
        }
        @Override public Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(Map<String, Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { 
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability); 
        }
        @Override public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { 
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability); 
        }
        @Override public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { 
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability); 
        }
        @Override public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { 
            return delegate.prepareStatement(sql, autoGeneratedKeys); 
        }
        @Override public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { 
            return delegate.prepareStatement(sql, columnIndexes); 
        }
        @Override public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { 
            return delegate.prepareStatement(sql, columnNames); 
        }
        @Override public Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws SQLClientInfoException { delegate.setClientInfo(name, value); }
        @Override public void setClientInfo(Properties properties) throws SQLClientInfoException { delegate.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException { delegate.setNetworkTimeout(executor, milliseconds); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
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
    
    public static Connection getConnection() throws SQLException {
        if (!configLoaded) {
            throw new SQLException("Database not configured. Please run the setup wizard or check config file.\n" + 
                                   "Error: " + configError);
        }

        long startTime = System.currentTimeMillis();

        while (activeConnections >= MAX_CONNECTIONS) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > CONNECTION_TIMEOUT_MS) {
                logConnectionLeaks();
                throw new SQLException("Connection timeout after " + (elapsed/1000) + 
                                       " seconds. All " + MAX_CONNECTIONS + 
                                       " connections are busy. Please try again later.\n" +
                                       "Active connections: " + activeConnections);
            }
            try {
                LOGGER.fine("Waiting for connection... Active: " + activeConnections + 
                           "/" + MAX_CONNECTIONS + " (waited " + (elapsed/1000) + "s)");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for database connection");
            }
        }

        try {
            Properties connProps = new Properties();
            connProps.setProperty("user", USER);
            connProps.setProperty("password", PASSWORD);
            connProps.setProperty("useSSL", "true");
            connProps.setProperty("serverTimezone", "UTC");
            connProps.setProperty("useUnicode", "true");
            connProps.setProperty("characterEncoding", "UTF-8");
            connProps.setProperty("connectTimeout", "10000");
            connProps.setProperty("socketTimeout", "60000");

            Connection rawConn = DriverManager.getConnection(URL, connProps);
            
            // Wrap the connection to track closure
            TrackedConnection trackedConn = new TrackedConnection(rawConn);
            
            activeConnections++;
            totalConnectionsCreated++;
            
            // Track connection for leak detection
            connectionTraces.put(trackedConn, Thread.currentThread().getStackTrace());

            // Enhanced logging to show which method created the connection
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String caller = "unknown";
            if (stack.length > 2) {
                caller = stack[2].getClassName() + "." + stack[2].getMethodName() + ":" + stack[2].getLineNumber();
            }

            LOGGER.info("🔌 Connection CREATED. Active: " + activeConnections + 
                       "/" + MAX_CONNECTIONS + ", Total: " + totalConnectionsCreated +
                       " | Called by: " + caller);

            return trackedConn;

        } catch (SQLException e) {
            String safeMessage = getSafeErrorMessage(e);
            LOGGER.log(Level.SEVERE, "Database connection failed: " + safeMessage);
            throw new SQLException("Unable to connect to the database. Please check:\n" +
                                   "1. MySQL server is running\n" +
                                   "2. Database credentials are correct\n" +
                                   "3. Database 'dental_clinic_db' exists");
        }
    }
    
    /**
     * Release connection back to pool (called automatically when wrapped connection is closed)
     */
    public static void releaseConnection(Connection conn, StackTraceElement[] creationTrace) {
        if (conn != null) {
            // Get caller info
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String caller = "unknown";
            if (stack.length > 2) {
                caller = stack[2].getClassName() + "." + stack[2].getMethodName() + ":" + stack[2].getLineNumber();
            }
            
            activeConnections--;
            connectionTraces.remove(conn);
            LOGGER.info("🔌 Connection RELEASED. Active: " + activeConnections + 
                       "/" + MAX_CONNECTIONS + " | Released by: " + caller);
        }
    }
    
    // ==========================================================
    // HELPER METHODS
    // ==========================================================
    
    private static void logConnectionLeaks() {
        LOGGER.warning("=== CONNECTION LEAK DETECTED ===");
        LOGGER.warning("Active connections: " + activeConnections + "/" + MAX_CONNECTIONS);
        LOGGER.warning("Connection traces:");
        
        for (Map.Entry<Connection, StackTraceElement[]> entry : connectionTraces.entrySet()) {
            LOGGER.warning("Connection: " + entry.getKey().toString());
            StackTraceElement[] stack = entry.getValue();
            int lines = Math.min(stack.length, 10);
            for (int i = 0; i < lines; i++) {
                LOGGER.warning("  " + stack[i].toString());
            }
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
        return String.format("Active: %d/%d, Total Created: %d, Configured: %s",
            activeConnections, MAX_CONNECTIONS, totalConnectionsCreated, configLoaded ? "Yes" : "No");
    }
    
    public static void logActiveConnections() {
        System.err.println("=== ACTIVE CONNECTIONS: " + activeConnections + "/" + MAX_CONNECTIONS + " ===");
        if (activeConnections > 0) {
            System.err.println("Connection traces:");
            for (Map.Entry<Connection, StackTraceElement[]> entry : connectionTraces.entrySet()) {
                System.err.println("  Connection: " + entry.getKey());
                StackTraceElement[] stack = entry.getValue();
                for (int i = 0; i < Math.min(stack.length, 5); i++) {
                    System.err.println("    " + stack[i]);
                }
            }
        }
    }
    
    public static int getActiveConnectionCount() {
        return activeConnections;
    }
}