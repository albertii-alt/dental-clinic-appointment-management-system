package com.dentalclinic.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Ensure you have created 'dental_clinic_db' in your MySQL/XAMPP
    private static final String URL = "jdbc:mysql://localhost:3306/dental_clinic_db";
    private static final String USER = "root"; 
    private static final String PASS = ""; 

    // Inside DBConnection.java
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            // You could call a static method here to insert this error 
            // into the system_logs table (if the DB is even reachable)
            System.err.println("DATABASE ERROR: " + e.getMessage());
            throw e;
        }
    }
}