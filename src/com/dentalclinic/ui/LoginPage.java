package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.ui.PatientDashboard;// Ensure this import exists
import com.dentalclinic.service.AuthService;
import com.dentalclinic.model.Patient;

public class LoginPage extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JComboBox<String> roleDropdown;

    public LoginPage() {
        setTitle("Dental Clinic Appointment Management System");
        setSize(400, 300);
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
        
        // --- ADD THIS ABOVE THE BUTTONS IN THE CONSTRUCTOR ---
        JLabel roleLabel = new JLabel("Login as:");
        roleLabel.setBounds(50, 150, 80, 25);
        panel.add(roleLabel);

        String[] roles = {"Patient", "Staff", "Dentist", "Admin"};
        roleDropdown = new JComboBox<>(roles);
        roleDropdown.setBounds(140, 150, 180, 25);
        panel.add(roleDropdown);


        // --- BUTTONS ---
        loginButton = new JButton("Login");
        loginButton.setBounds(120, 200, 80, 30);
        panel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(220, 200, 90, 30);
        panel.add(registerButton);
 
        add(panel);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String selectedRole = (String) roleDropdown.getSelectedItem();

            AuthService authService = new AuthService();

            try {
                Object result = authService.login(username, password, selectedRole);

                if (result instanceof Object[]) {
                    Object[] data = (Object[]) result;
                    int loggedId = (int) data[0];      // staff_id
                    String role = (String) data[1];    // role (ADMIN/STAFF/DENTIST)
                    boolean isSuper = (boolean) data[2];
                    String fullName = (String) data[3]; // The real name from the updated StaffDAO
                    String userEmail = (data.length > 4) ? (String) data[4] : "No Email";

                    // 1. Initialize the Global Session so logs know WHO is doing the action
                    com.dentalclinic.util.UserSession.initialize(loggedId, fullName, role);

                    // 2. Route to the correct Dashboard
                    if (role.equalsIgnoreCase("ADMIN")) {
                        new com.dentalclinic.ui.AdminDashboard(loggedId, isSuper, fullName, userEmail, username);
                    } 
                    else if (role.equalsIgnoreCase("DENTIST")) {
                        // Change staffId to loggedId AND staffName to fullName
                        new com.dentalclinic.ui.DentistDashboard(loggedId, fullName, username, userEmail);
                    }
                    else if (role.equalsIgnoreCase("STAFF")) {
                        // Pass both loggedId and fullName (from your data[3] extraction)
                        new com.dentalclinic.ui.StaffDashboard(loggedId, fullName, username, userEmail);
                    }
                    
                    dispose(); // Close login page
                } 
                else if (result instanceof Patient) {
                    Patient p = (Patient) result;
                    
                    // Initialize Session for Patient
                    String patientFullName = p.getFirstName() + " " + p.getLastName();
                    com.dentalclinic.util.UserSession.initialize(p.getPatientId(), patientFullName, "PATIENT");

                    JOptionPane.showMessageDialog(null, "Login Successful! Welcome, " + p.getFirstName());
                    
                    new PatientDashboard(
                        p.getPatientId(), p.getFirstName(), p.getMiddleName(), 
                        p.getLastName(), p.getBirthDate().toString(), 
                        String.valueOf(p.getAge()), p.getAddress(), 
                        p.getContactNumber(), p.getUsername()
                    );
                    
                    dispose(); // Close login page
                } 
                else {
                    JOptionPane.showMessageDialog(null, "Invalid Username or Password for the selected role.", 
                                                  "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Database Connection Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        registerButton.addActionListener(e -> {
            new com.dentalclinic.patient.RegisterPatientForm();
            dispose();
        });

        setVisible(true);
    }
}