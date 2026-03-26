package com.dentalclinic.staff;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
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

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(210, 215, 220);

    public RegisterPatientPanel() {
        setLayout(new GridBagLayout());
        setBackground(BG);

        // --- THE FORM CONTAINER (LANDSCAPE OPTIMIZED) ---
        JPanel container = new JPanel();
        container.setLayout(new BorderLayout(0, 20));
        container.setPreferredSize(new Dimension(850, 550)); 
        container.setBackground(CARD);
        container.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));

        // HEADER
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(CARD);
        JLabel title = new JLabel("Walk-in Patient Registration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("Fill out the information below to register a new patient.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        
        header.add(title);
        header.add(subtitle);
        container.add(header, BorderLayout.NORTH);

        // --- TWO-COLUMN FORM GRID ---
        JPanel formGrid = new JPanel(new GridLayout(0, 2, 40, 15));
        formGrid.setBackground(CARD);

        // Column 1: Personal Details
        formGrid.add(createFieldGroup("First Name", firstNameField = new JTextField()));
        formGrid.add(createFieldGroup("Middle Name", middleNameField = new JTextField()));
        formGrid.add(createFieldGroup("Last Name", lastNameField = new JTextField()));
        
        // Birth Date Group
        JPanel dobGroup = new JPanel(new BorderLayout(0, 5));
        dobGroup.setBackground(CARD);
        dobGroup.add(createLabelOnly("Birth Date"), BorderLayout.NORTH);
        birthDatePicker = new JDateChooser();
        birthDatePicker.setDateFormatString("MMMM d, yyyy");
        birthDatePicker.setPreferredSize(new Dimension(0, 35));
        dobGroup.add(birthDatePicker, BorderLayout.CENTER);
        formGrid.add(dobGroup);

        formGrid.add(createFieldGroup("Calculated Age", ageField = new JTextField()));
        ageField.setEditable(false);
        ageField.setBackground(new Color(245, 245, 245));

        formGrid.add(createFieldGroup("Full Address", addressField = new JTextField()));

        // Column 2: Contact & Account
        formGrid.add(createFieldGroup("Contact Number (11 Digits)", contactField = new JTextField()));
        formGrid.add(createFieldGroup("Email Address", emailField = new JTextField()));
        formGrid.add(createFieldGroup("Username", usernameField = new JTextField()));
        
        // Password Group
        JPanel passGroup = new JPanel(new BorderLayout(0, 5));
        passGroup.setBackground(CARD);
        passGroup.add(createLabelOnly("Account Password"), BorderLayout.NORTH);
        passwordField = new JPasswordField();
        styleInputField(passwordField);
        passGroup.add(passwordField, BorderLayout.CENTER);
        formGrid.add(passGroup);

        container.add(formGrid, BorderLayout.CENTER);

        // --- FOOTER BUTTONS ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        footer.setBackground(CARD);

        clearBtn = new JButton("Clear Form");
        styleButton(clearBtn, new Color(160, 170, 180));
        
        submitBtn = new JButton("Register Patient");
        styleButton(submitBtn, SUCCESS);
        submitBtn.setPreferredSize(new Dimension(180, 40));

        footer.add(clearBtn);
        footer.add(submitBtn);
        container.add(footer, BorderLayout.SOUTH);

        // --- LISTENERS ---
        contactField.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar()) || contactField.getText().length() >= 11) {
                    e.consume();
                }
            }
        });

        birthDatePicker.addPropertyChangeListener("date", evt -> {
            if (birthDatePicker.getDate() != null) {
                int age = calculateAge(birthDatePicker.getDate());
                ageField.setText(String.valueOf(age));
            }
        });

        submitBtn.addActionListener(e -> handleStaffRegistration());
        clearBtn.addActionListener(e -> clearFields());

        add(container);
    }

    // --- UI HELPERS ---

    private JPanel createFieldGroup(String labelText, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(CARD);
        p.add(createLabelOnly(labelText), BorderLayout.NORTH);
        styleInputField(field);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JLabel createLabelOnly(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT);
        return lbl;
    }

    private void styleInputField(JTextField field) {
        field.setPreferredSize(new Dimension(0, 35));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 25, 10, 25));
    }

    // --- LOGIC (UNCHANGED) ---

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