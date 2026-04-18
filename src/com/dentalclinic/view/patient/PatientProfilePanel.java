package com.dentalclinic.view.patient;

import com.dentalclinic.controller.PatientController;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import com.toedter.calendar.JDateChooser;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.PasswordValidator;
import com.dentalclinic.util.Sanitizer;  // ADDED: Import Sanitizer
import java.util.List;

public class PatientProfilePanel extends JPanel {
    private JTextField txtFName, txtMName, txtLName, txtAge, txtAddr, txtPhone, txtEmail, txtUser;
    private JDateChooser birthDatePicker;
    private JPasswordField txtCurrentPass, txtNewPass, txtConfirmPass;
    private final PatientController patientController = new PatientController();
    private int patientID;
    
    // SECURITY: Input limits
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_ADDRESS_LENGTH = 200;
    private static final int MAX_CONTACT_LENGTH = 11;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_USERNAME_LENGTH = 50;

    public PatientProfilePanel(int pID) {
        this.patientID = pID;
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        JLabel header = new JLabel("My Profile Settings");
        header.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.setForeground(new Color(44, 62, 80));
        add(header, BorderLayout.NORTH);

        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setOpaque(false);

        setupUI(formContainer);

        JScrollPane scroll = new JScrollPane(formContainer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        JButton btnSave = new JButton("Save All Changes");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnSave.setBackground(new Color(41, 128, 185));
        btnSave.setForeground(Color.WHITE);
        btnSave.setPreferredSize(new Dimension(0, 50));
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> handleUpdate());
        add(btnSave, BorderLayout.SOUTH);
    }

    private void setupUI(JPanel container) {
        try {
            Patient p = patientController.getPatientById(patientID);
            if (p == null) return;

            JPanel generalPnl = createSection("General Information");
            GridBagConstraints gbc = createGBC();

            txtFName = addField(generalPnl, "First Name:", p.getFirstName(), gbc, 0);
            limitTextFieldLength(txtFName, MAX_NAME_LENGTH);
            
            txtMName = addField(generalPnl, "Middle Name:", p.getMiddleName(), gbc, 1);
            limitTextFieldLength(txtMName, MAX_NAME_LENGTH);
            
            txtLName = addField(generalPnl, "Last Name:", p.getLastName(), gbc, 2);
            limitTextFieldLength(txtLName, MAX_NAME_LENGTH);

            gbc.gridx = 0; gbc.gridy = 3;
            generalPnl.add(new JLabel("Birth Date:"), gbc);
            birthDatePicker = new JDateChooser();
            birthDatePicker.setDateFormatString("MMMM d, yyyy");
            birthDatePicker.setDate(p.getBirthDate());
            gbc.gridx = 1; generalPnl.add(birthDatePicker, gbc);

            gbc.gridx = 0; gbc.gridy = 4;
            generalPnl.add(new JLabel("Current Age:"), gbc);
            txtAge = new JTextField(String.valueOf(p.getAge()));
            txtAge.setEditable(false);
            txtAge.setBackground(new Color(236, 240, 241));
            gbc.gridx = 1; generalPnl.add(txtAge, gbc);

            birthDatePicker.addPropertyChangeListener("date", evt -> {
                if (birthDatePicker.getDate() != null) {
                    txtAge.setText(String.valueOf(calculateAge(birthDatePicker.getDate())));
                }
            });

            txtAddr = addField(generalPnl, "Full Address:", p.getAddress(), gbc, 5);
            limitTextFieldLength(txtAddr, MAX_ADDRESS_LENGTH);
            
            txtPhone = addField(generalPnl, "Contact No:", p.getContactNumber(), gbc, 6);
            limitTextFieldLength(txtPhone, MAX_CONTACT_LENGTH);
            // SECURITY: Only allow digits
            txtPhone.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyTyped(java.awt.event.KeyEvent evt) {
                    char c = evt.getKeyChar();
                    if (!Character.isDigit(c) || txtPhone.getText().length() >= MAX_CONTACT_LENGTH) {
                        evt.consume();
                    }
                }
            });
            
            txtEmail = addField(generalPnl, "Email Address:", p.getEmail(), gbc, 7);
            limitTextFieldLength(txtEmail, MAX_EMAIL_LENGTH);
            
            txtUser = addField(generalPnl, "Username:", p.getUsername(), gbc, 8);
            limitTextFieldLength(txtUser, MAX_USERNAME_LENGTH);

            container.add(generalPnl);
            container.add(Box.createVerticalStrut(20));

            JPanel securityPnl = createSection("Account Security");
            GridBagConstraints sGbc = createGBC();

            JLabel lblSec = new JLabel("Verification required to save changes");
            lblSec.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblSec.setForeground(new Color(192, 57, 43));
            sGbc.gridwidth = 2; sGbc.gridy = 0; sGbc.gridx = 0;
            securityPnl.add(lblSec, sGbc);
            sGbc.gridwidth = 1;

            txtCurrentPass = addPassField(securityPnl, "Current Password:", sGbc, 1);
            txtNewPass = addPassField(securityPnl, "New Password (Optional):", sGbc, 2);
            txtConfirmPass = addPassField(securityPnl, "Confirm New Password:", sGbc, 3);

            container.add(securityPnl);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private JPanel createSection(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        TitledBorder tb = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 230, 235)), title);
        tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        tb.setTitleColor(new Color(41, 128, 185));
        p.setBorder(new CompoundBorder(tb, new EmptyBorder(15, 20, 15, 20)));
        return p;
    }

    private GridBagConstraints createGBC() {
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(6, 6, 6, 6);
        return g;
    }

    private JTextField addField(JPanel p, String lbl, String val, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        p.add(new JLabel(lbl), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JTextField t = new JTextField(val, 20);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        p.add(t, gbc);
        return t;
    }

    private JPasswordField addPassField(JPanel p, String lbl, GridBagConstraints gbc, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        p.add(new JLabel(lbl), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        JPasswordField t = new JPasswordField(20);
        p.add(t, gbc);
        return t;
    }
    
    // SECURITY: Limit text field length
    private void limitTextFieldLength(JTextField field, int maxLength) {
        field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                if (field.getText().length() >= maxLength) {
                    evt.consume();
                }
            }
        });
    }
    
    // REMOVED: Old sanitizeInput() method - replaced with Sanitizer utility
    // private String sanitizeInput(String input) { ... }  // DELETED

    private int calculateAge(java.util.Date birthDate) {
        java.time.LocalDate birth = java.time.Instant.ofEpochMilli(birthDate.getTime())
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
        return java.time.Period.between(birth, java.time.LocalDate.now()).getYears();
    }

    private void handleUpdate() {
        String currentPass = new String(txtCurrentPass.getPassword());
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());
        
        // ==========================================================
        // FIXED: Replaced sanitizeInput() with Sanitizer utility
        // ==========================================================
        
        // Get raw inputs
        String rawFName = txtFName.getText().trim();
        String rawMName = txtMName.getText().trim();
        String rawLName = txtLName.getText().trim();
        String rawAddress = txtAddr.getText().trim();
        String rawPhone = txtPhone.getText().trim();
        String rawEmail = txtEmail.getText().trim();
        String rawUsername = txtUser.getText().trim();

        // APPLY SANITIZER to all text fields
        String fName = Sanitizer.sanitizeName(rawFName);
        String mName = Sanitizer.sanitizeName(rawMName);
        String lName = Sanitizer.sanitizeName(rawLName);
        String address = Sanitizer.sanitizeTextField(rawAddress);
        String phone = Sanitizer.sanitizePhone(rawPhone);
        String email = Sanitizer.sanitizeEmail(rawEmail);
        String username = Sanitizer.sanitizeUsername(rawUsername);

        try {
            // Validate required fields
            if (fName.isEmpty() || lName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "First name and last name are required.");
                return;
            }
            
            // SECURITY: Validate phone number (using Sanitizer's validation)
            if (!Sanitizer.isValidPhone(phone)) {
                JOptionPane.showMessageDialog(this, "Please enter a valid contact number (8-20 digits, may include + - space).");
                return;
            }
            
            // SECURITY: Validate email (using Sanitizer's validation)
            if (!rawEmail.isEmpty() && !Sanitizer.isValidEmail(email)) {
                JOptionPane.showMessageDialog(this, "Please enter a valid email address.");
                return;
            }
            
            // SECURITY: Validate username (using Sanitizer's validation)
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username is required.");
                return;
            }
            
            if (!Sanitizer.isValidUsername(username)) {
                JOptionPane.showMessageDialog(this, "Username must be 3-50 characters (letters, numbers, _, ., -).");
                return;
            }
            
            // Verify current password
            if (!patientController.verifyPassword(patientID, currentPass)) {
                JOptionPane.showMessageDialog(this, "Verification Failed: Current password is incorrect.", "Security", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String passToSave = null;
            if (!newPass.isEmpty()) {
                if (!newPass.equals(confirmPass)) {
                    JOptionPane.showMessageDialog(this, "New passwords do not match!");
                    return;
                }
                
                // SECURITY: Validate new password complexity
                List<String> passwordErrors = PasswordValidator.validatePassword(newPass);
                if (!passwordErrors.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
                    for (String error : passwordErrors) {
                        errorMsg.append("• ").append(error).append("\n");
                    }
                    JOptionPane.showMessageDialog(this, errorMsg.toString(), "Invalid Password", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                passToSave = newPass;
            }

            boolean success = patientController.updateFullProfile(
                patientID, fName, mName, lName,
                birthDatePicker.getDate(), Integer.parseInt(txtAge.getText()), address, 
                phone, email, username, passToSave
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Profile updated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "Update failed. Username may already exist.");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid age format.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
