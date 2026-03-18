package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.dentalclinic.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginPage extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;

    public LoginPage() {
        setTitle("Dental Clinic Appointment Management System");
        setSize(400, 300); // Reverted height since Role Selection is removed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Dental Clinic Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBounds(110, 20, 200, 30);
        panel.add(titleLabel);

        // --- USERNAME ---
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(50, 80, 80, 25);
        panel.add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(140, 80, 180, 25);
        panel.add(usernameField);

        // --- PASSWORD ---
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(50, 120, 80, 25);
        panel.add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(140, 120, 180, 25);
        panel.add(passwordField);

        // --- BUTTONS ---
        loginButton = new JButton("Login");
        loginButton.setBounds(140, 170, 80, 30);
        panel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(240, 170, 90, 30);
        panel.add(registerButton);

        add(panel);

        // Login button action
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                // 1. Check for Hardcoded System Users first
                if (username.equals("admin") && password.equals("1234")) {
                    new AdminDashboard();
                    dispose();
                } else if (username.equals("dentist") && password.equals("1234")) {
                    new DentistDashboard();
                    dispose();
                } else if (username.equals("staff") && password.equals("1234")) {
                    new StaffDashboard();
                    dispose();
                } 
                // 2. Check Database for Patient Credentials
                else {
                    String query = "SELECT * FROM patients WHERE username = ? AND password = ?";

                    try (Connection conn = DBConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(query)) {

                        pstmt.setString(1, username);
                        pstmt.setString(2, password);

                        ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                       // Success! Patient found in DB
                       String name = rs.getString("first_name");
                       JOptionPane.showMessageDialog(null, "Login Successful! Welcome, " + name);

                       // Call the new Patient Dashboard and pass the name
                       new PatientDashboard(name); 
                       dispose();
                   }else {
                            // No match found in hardcoded list OR database
                            JOptionPane.showMessageDialog(null, "Invalid Username or Password", "Login Failed", JOptionPane.ERROR_MESSAGE);
                        }

                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        });

        registerButton.addActionListener(e -> {
            new com.dentalclinic.patient.RegisterPatientForm();
            dispose();
        });

        setVisible(true);
    }
}