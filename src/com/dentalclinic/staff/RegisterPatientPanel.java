package com.dentalclinic.staff;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.dentalclinic.service.AuthService;

public class RegisterPatientPanel extends JPanel {

    private JTextField firstNameField, middleNameField, lastNameField;
    private JTextField emailField, usernameField, addressField, contactField, ageField;
    private JPasswordField passwordField;
    private JDateChooser birthDatePicker;
    private JButton submitBtn, clearBtn;
    private AuthService authService = new AuthService();

    public RegisterPatientPanel() {
        // Using GridBagLayout to perfectly center the form container
        setLayout(new GridBagLayout());
        setBackground(new Color(236, 240, 241));

        // --- THE FORM CONTAINER ---
        JPanel formContainer = new JPanel(null);
        formContainer.setPreferredSize(new Dimension(450, 600));
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));

        JLabel title = new JLabel("Walk-in Registration", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setBounds(0, 20, 450, 30);
        formContainer.add(title);

        // --- FORM FIELDS ---
        addLabelAndField(formContainer, "First Name:", firstNameField = new JTextField(), 70);
        addLabelAndField(formContainer, "Middle Name:", middleNameField = new JTextField(), 110);
        addLabelAndField(formContainer, "Last Name:", lastNameField = new JTextField(), 150);

        JLabel dobLabel = new JLabel("Birth Date:");
        dobLabel.setBounds(50, 190, 100, 25);
        formContainer.add(dobLabel);
        
        birthDatePicker = new JDateChooser();
        birthDatePicker.setDateFormatString("MMMM d, yyyy");
        birthDatePicker.setBounds(160, 190, 240, 25);
        formContainer.add(birthDatePicker);

        addLabelAndField(formContainer, "Age:", ageField = new JTextField(), 230);
        ageField.setEditable(false);
        ageField.setBackground(new Color(240, 240, 240));

        addLabelAndField(formContainer, "Address:", addressField = new JTextField(), 270);
        addLabelAndField(formContainer, "Contact No:", contactField = new JTextField(), 310);
        
        // 1. CONTACT NUMBER LIMIT (11 Characters + Digits only)
        contactField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar()) || contactField.getText().length() >= 11) {
                    e.consume();
                }
            }
        });

        addLabelAndField(formContainer, "Email:", emailField = new JTextField(), 350);
        addLabelAndField(formContainer, "Username:", usernameField = new JTextField(), 390);
        
        // 2. REMOVED TEMPORARY PASSWORD - Just a blank field for the staff to set
        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(50, 430, 100, 25);
        formContainer.add(passLabel);
        passwordField = new JPasswordField();
        passwordField.setBounds(160, 430, 240, 25);
        formContainer.add(passwordField);

        // --- BUTTONS ---
        submitBtn = new JButton("Register");
        submitBtn.setBounds(160, 490, 110, 35);
        submitBtn.setBackground(new Color(46, 204, 113));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.addActionListener(e -> handleStaffRegistration());
        formContainer.add(submitBtn);

        clearBtn = new JButton("Clear");
        clearBtn.setBounds(290, 490, 110, 35);
        clearBtn.addActionListener(e -> clearFields());
        formContainer.add(clearBtn);

        // --- LISTENERS ---
        birthDatePicker.addPropertyChangeListener("date", evt -> {
            if (birthDatePicker.getDate() != null) {
                int age = calculateAge(birthDatePicker.getDate());
                ageField.setText(String.valueOf(age));
            }
        });

        // Add the container to the centered GridBagLayout
        add(formContainer);
    }

    private void addLabelAndField(JPanel panel, String labelText, JTextField field, int yPos) {
        JLabel label = new JLabel(labelText);
        label.setBounds(50, yPos, 100, 25);
        panel.add(label);
        field.setBounds(160, yPos, 240, 25);
        panel.add(field);
    }

    private int calculateAge(java.util.Date birthDate) {
        java.time.LocalDate birth = new java.sql.Date(birthDate.getTime()).toLocalDate();
        return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
    }

    private void handleStaffRegistration() {
        String pass = new String(passwordField.getPassword());
        if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty() || 
            usernameField.getText().isEmpty() || pass.isEmpty() || birthDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields.");
            return;
        }

        if (contactField.getText().length() != 11) {
            JOptionPane.showMessageDialog(this, "Contact number must be exactly 11 digits.");
            return;
        }

        try {
            java.sql.Date sqlDate = new java.sql.Date(birthDatePicker.getDate().getTime());
            boolean success = authService.registerNewPatient(
                firstNameField.getText(), middleNameField.getText(), lastNameField.getText(),
                sqlDate, Integer.parseInt(ageField.getText()), addressField.getText(),
                contactField.getText(), emailField.getText(), usernameField.getText(), pass
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Patient Registered Successfully!");
                clearFields();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void clearFields() {
        firstNameField.setText("");
        middleNameField.setText("");
        lastNameField.setText("");
        ageField.setText("");
        addressField.setText("");
        contactField.setText("");
        emailField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        birthDatePicker.setDate(null);
    }
}