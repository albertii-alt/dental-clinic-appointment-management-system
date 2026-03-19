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
    private JTextField addressField, contactField, ageField;

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
        
        
        JLabel ageLabel = new JLabel("Age:");
        ageLabel.setBounds(50, 230, 100, 25);
        panel.add(ageLabel);
        
        ageField = new JTextField();
        ageField.setBounds(160, 230, 200, 25);
        ageField.setEditable(false); 
        ageField.setBackground(new Color(230, 230, 230)); 
        panel.add(ageField);

        // 3. THE LISTENER (Put it here!)
        birthDatePicker.addPropertyChangeListener("date", evt -> {
            if (birthDatePicker.getDate() != null) {
                // This code runs every time the user clicks a date in the calendar
                int age = calculateAge(birthDatePicker.getDate());
                ageField.setText(String.valueOf(age));
            }
        });
        

        addLabelAndField(panel, "Full Address:", addressField = new JTextField(), 270);
        addLabelAndField(panel, "Contact No:", contactField = new JTextField(), 310);

        contactField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                // Only allow numbers and limit to 11 characters
                if (!Character.isDigit(evt.getKeyChar()) || contactField.getText().length() >= 11) {
                    evt.consume(); 
                }
            }
        });

        // --- SHIFTED EXISTING FIELDS (Moving down to make room) ---
        addLabelAndField(panel, "Email Address:", emailField = new JTextField(), 350);
        addLabelAndField(panel, "Username:", usernameField = new JTextField(), 390);
        
 // --- PASSWORDS (y=430, 470) ---
        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 430, 100, 25);
        panel.add(passLabel);
        passwordField = new JPasswordField();
        passwordField.setBounds(160, 430, 200, 25);
        panel.add(passwordField);

        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPassLabel.setBounds(50, 470, 100, 25);
        panel.add(confirmPassLabel);
        confirmPasswordField = new JPasswordField();
        confirmPasswordField.setBounds(160, 470, 200, 25);
        panel.add(confirmPasswordField);

        // --- BUTTONS (y=530) ---
        submitBtn = new JButton("Submit");
        submitBtn.setBounds(160, 530, 90, 35);
        submitBtn.setBackground(new Color(52, 152, 219));
        submitBtn.setForeground(Color.WHITE);
        panel.add(submitBtn);

        cancelBtn = new JButton("Cancel");
        cancelBtn.setBounds(270, 530, 90, 35);
        panel.add(cancelBtn);
        
        birthDatePicker.addPropertyChangeListener("date", evt -> {
            if (birthDatePicker.getDate() != null) {
                int age = calculateAge(birthDatePicker.getDate());
                ageField.setText(String.valueOf(age));
            }
        });

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
    
    private int calculateAge(java.util.Date birthDate) {
    java.time.LocalDate birth = new java.sql.Date(birthDate.getTime()).toLocalDate();
    java.time.LocalDate now = java.time.LocalDate.now();
    return java.time.Period.between(birth, now).getYears();
    }

        private void handleRegistration() {
                String fName = firstNameField.getText();
                String mName = middleNameField.getText();
                String lName = lastNameField.getText();
                String address = addressField.getText();
                String contact = contactField.getText(); 
                String email = emailField.getText();
                String user = usernameField.getText();
                String pass = new String(passwordField.getPassword());
                String confirmPass = new String(confirmPasswordField.getPassword());

                // 1. Validation (Add address and contact to check)
                if (fName.isEmpty() || lName.isEmpty() || address.isEmpty() || contact.isEmpty() || user.isEmpty() || pass.isEmpty() || birthDatePicker.getDate() == null) {
                    JOptionPane.showMessageDialog(this, "All required fields must be filled!");
                    return;
                }
                // Add this after your empty field checks
                if (contact.length() != 11) {
                    JOptionPane.showMessageDialog(this, "Contact number must be exactly 11 digits (e.g., 09123456789)");
                    return;
                }

                // Optional: Check if it's only numbers
                if (!contact.matches("\\d+")) {
                    JOptionPane.showMessageDialog(this, "Contact number must only contain digits.");
                    return;
                }

        // 2. Database Insertion
        String query = "INSERT INTO patients (first_name, middle_name, last_name, birth_date, age, address, contact_number, email, username, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, fName);
            pstmt.setString(2, mName);
            pstmt.setString(3, lName);
            
            // Convert JDateChooser date to SQL date
            java.util.Date utilDate = birthDatePicker.getDate();
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            pstmt.setDate(4, sqlDate);
            pstmt.setInt(5, Integer.parseInt(ageField.getText()));
            pstmt.setString(6, address);
            pstmt.setString(7, contact);
            pstmt.setString(8, email);
            pstmt.setString(9, user);
            pstmt.setString(10, pass); // Note: In real apps, we'd hash this!

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