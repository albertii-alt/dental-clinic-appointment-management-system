package com.dentalclinic.patient;

import com.dentalclinic.ui.LoginPage;
import com.dentalclinic.util.DBConnection; // Import our bridge
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterPatientForm extends JFrame {

    private JTextField firstNameField, middleNameField, lastNameField;
    private JTextField emailField, usernameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JDateChooser birthDatePicker;
    private JButton submitBtn, cancelBtn;

    public RegisterPatientForm() {
        setTitle("Patient Registration - Register Use Case");
        setSize(450, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(new Color(245, 245, 245));

        JLabel title = new JLabel("Create New Account");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setBounds(110, 20, 250, 30);
        panel.add(title);

        addLabelAndField(panel, "First Name:", firstNameField = new JTextField(), 70);
        addLabelAndField(panel, "Middle Name:", middleNameField = new JTextField(), 110);
        addLabelAndField(panel, "Last Name:", lastNameField = new JTextField(), 150);

        JLabel dobLabel = new JLabel("Birth Date:");
        dobLabel.setBounds(50, 190, 100, 25);
        panel.add(dobLabel);
        
        birthDatePicker = new JDateChooser();
        birthDatePicker.setDateFormatString("MMMM d, yyyy");
        birthDatePicker.setBounds(160, 190, 200, 25);
        panel.add(birthDatePicker);

        addLabelAndField(panel, "Email Address:", emailField = new JTextField(), 230);
        addLabelAndField(panel, "Username:", usernameField = new JTextField(), 270);
        
        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 310, 100, 25);
        panel.add(passLabel);
        passwordField = new JPasswordField();
        passwordField.setBounds(160, 310, 200, 25);
        panel.add(passwordField);

        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPassLabel.setBounds(50, 350, 100, 25);
        panel.add(confirmPassLabel);
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setBounds(160, 350, 200, 25);
        panel.add(confirmPasswordField);

        submitBtn = new JButton("Submit");
        submitBtn.setBounds(160, 410, 90, 35);
        submitBtn.setBackground(new Color(52, 152, 219)); // Matched your Dashboard Blue
        submitBtn.setForeground(Color.WHITE);
        panel.add(submitBtn);

        cancelBtn = new JButton("Cancel");
        cancelBtn.setBounds(270, 410, 90, 35);
        panel.add(cancelBtn);

        add(panel);

        submitBtn.addActionListener(e -> handleRegistration());
        cancelBtn.addActionListener(e -> { new LoginPage(); dispose(); });

        setVisible(true);
    }

    private void addLabelAndField(JPanel panel, String labelText, JTextField field, int yPos) {
        JLabel label = new JLabel(labelText);
        label.setBounds(50, yPos, 100, 25);
        panel.add(label);
        field.setBounds(160, yPos, 200, 25);
        panel.add(field);
    }

    private void handleRegistration() {
        String fName = firstNameField.getText();
        String mName = middleNameField.getText();
        String lName = lastNameField.getText();
        String email = emailField.getText();
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        // 1. Validation
        if (fName.isEmpty() || lName.isEmpty() || user.isEmpty() || pass.isEmpty() || birthDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "All required fields must be filled!");
            return;
        }
        if (!pass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match!");
            return;
        }

        // 2. Database Insertion
        String query = "INSERT INTO patients (first_name, middle_name, last_name, birth_date, email, username, password) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, fName);
            pstmt.setString(2, mName);
            pstmt.setString(3, lName);
            
            // Convert JDateChooser date to SQL date
            java.util.Date utilDate = birthDatePicker.getDate();
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            pstmt.setDate(4, sqlDate);
            
            pstmt.setString(5, email);
            pstmt.setString(6, user);
            pstmt.setString(7, pass); // Note: In real apps, we'd hash this!

            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Registration Successful for " + fName + " " + lName);
                new LoginPage();
                dispose();
            }

        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) { // MySQL Duplicate Entry Error
                JOptionPane.showMessageDialog(this, "Username already exists. Please choose another.");
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
            ex.printStackTrace();
        }
    }
}