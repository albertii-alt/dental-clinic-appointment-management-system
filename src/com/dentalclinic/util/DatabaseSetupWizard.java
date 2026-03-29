package com.dentalclinic.util;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

public class DatabaseSetupWizard {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".dental_clinic";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "db.properties";
    
    public static boolean showIfNeeded(JFrame parent) {
        // Check if config exists and works
        if (DBConnection.testConnection()) {
            return true;
        }
        
        // Show setup wizard
        return showSetupWizard(parent);
    }
    
    public static boolean showSetupWizard(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Database Setup", true);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());
        
        // Main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        
        // Title
        JLabel titleLabel = new JLabel("Database Configuration Required");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(41, 128, 185));
        gbc.gridwidth = 2;
        gbc.gridy = 0;
        mainPanel.add(titleLabel, gbc);
        
        // Info text
        JLabel infoLabel = new JLabel("<html>Please enter your database connection details.<br>These will be saved securely for future use.</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridy = 1;
        mainPanel.add(infoLabel, gbc);
        
        // Form fields
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Host:"), gbc);
        JTextField hostField = new JTextField("localhost", 20);
        gbc.gridx = 1;
        mainPanel.add(hostField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Port:"), gbc);
        JTextField portField = new JTextField("3306", 20);
        gbc.gridx = 1;
        mainPanel.add(portField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        mainPanel.add(new JLabel("Database Name:"), gbc);
        JTextField dbNameField = new JTextField("dental_clinic_db", 20);
        gbc.gridx = 1;
        mainPanel.add(dbNameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        mainPanel.add(new JLabel("Username:"), gbc);
        JTextField userField = new JTextField("dental_user", 20);
        gbc.gridx = 1;
        mainPanel.add(userField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 6;
        mainPanel.add(new JLabel("Password:"), gbc);
        JPasswordField passField = new JPasswordField(20);
        gbc.gridx = 1;
        mainPanel.add(passField, gbc);
        
        // Test connection button
        JButton testBtn = new JButton("Test Connection");
        testBtn.setBackground(new Color(52, 152, 219));
        testBtn.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        mainPanel.add(testBtn, gbc);
        
        JLabel testResultLabel = new JLabel(" ");
        testResultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridy = 8;
        mainPanel.add(testResultLabel, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton saveBtn = new JButton("Save Configuration");
        JButton cancelBtn = new JButton("Exit Application");
        
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(new Color(231, 76, 60));
        cancelBtn.setForeground(Color.WHITE);
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        // Test connection action
        testBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String dbName = dbNameField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            
            String testUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=true&serverTimezone=UTC", 
                host, port, dbName);
            
            try {
                java.sql.Connection testConn = java.sql.DriverManager.getConnection(testUrl, user, pass);
                testConn.close();
                testResultLabel.setText("✓ Connection successful!");
                testResultLabel.setForeground(new Color(46, 204, 113));
                saveBtn.setEnabled(true);
            } catch (Exception ex) {
                testResultLabel.setText("✗ Connection failed: " + ex.getMessage());
                testResultLabel.setForeground(new Color(231, 76, 60));
                saveBtn.setEnabled(false);
            }
        });
        
        // Save configuration
        saveBtn.addActionListener(e -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            String dbName = dbNameField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            
            if (saveConfig(host, port, dbName, user, pass)) {
                JOptionPane.showMessageDialog(dialog, 
                    "Configuration saved successfully!\nPlease restart the application.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                System.exit(0); // Exit to restart with new config
            } else {
                JOptionPane.showMessageDialog(dialog, 
                    "Failed to save configuration. Please check file permissions.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelBtn.addActionListener(e -> {
            System.exit(0);
        });
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
        
        return true;
    }
    
    private static boolean saveConfig(String host, String port, String dbName, String user, String pass) {
        try {
            // Create directory if it doesn't exist
            File configDir = new File(CONFIG_DIR);
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            Properties props = new Properties();
            String url = String.format("jdbc:mysql://%s:%s/%s?useSSL=true&serverTimezone=UTC", 
                host, port, dbName);
            props.setProperty("db.url", url);
            props.setProperty("db.user", user);
            props.setProperty("db.password", pass);
            
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "Dental Clinic Database Configuration - DO NOT SHARE");
            }
            
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}