package com.dentalclinic.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Ensure you have created 'dental_clinic_db' in your MySQL/XAMPP
    private static final String URL = "jdbc:mysql://localhost:3306/dental_clinic_db";
    private static final String USER = "root"; 
    private static final String PASS = ""; 

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}