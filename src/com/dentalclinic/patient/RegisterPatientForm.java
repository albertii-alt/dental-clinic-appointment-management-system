package com.dentalclinic.patient;

import com.dentalclinic.ui.LoginPage;
import com.dentalclinic.util.DBConnection; 
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.dao.PatientDAO;

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
        // Collect data from fields (KEEP YOUR EXISTING DATA COLLECTION)
        String fName = firstNameField.getText();
        String mName = middleNameField.getText();
        String lName = lastNameField.getText();
        String address = addressField.getText();
        String contact = contactField.getText(); 
        String email = emailField.getText();
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());

        // 1. Keep your existing Validation logic (Empty check, contact length check)
        if (fName.isEmpty() || lName.isEmpty() || address.isEmpty() || contact.isEmpty() || user.isEmpty() || pass.isEmpty() || birthDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "All required fields must be filled!");
            return;
        }

        if (contact.length() != 11) {
            JOptionPane.showMessageDialog(this, "Contact number must be exactly 11 digits.");
            return;
        }
    try {
            // Use the SERVICE instead of the DAO
            com.dentalclinic.service.AuthService authService = new com.dentalclinic.service.AuthService();

            java.util.Date utilDate = birthDatePicker.getDate();
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            int ageValue = Integer.parseInt(ageField.getText());

            // Call the Service
            boolean success = authService.registerNewPatient(
                fName, mName, lName, sqlDate, ageValue, address, contact, email, user, pass
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Registration Successful for " + fName);
                new LoginPage();
                dispose();
            }
        } catch (java.sql.SQLException ex) {
            // Catch duplicate username error or database issues
            if (ex.getMessage().contains("Duplicate entry")) {
                JOptionPane.showMessageDialog(this, "Username already exists. Please choose another.");
            } else {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "An error occurred: " + ex.getMessage());
        }
    }
}