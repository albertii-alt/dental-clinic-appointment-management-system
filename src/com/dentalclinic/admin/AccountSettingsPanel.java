package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import com.dentalclinic.dao.StaffDAO;
import com.dentalclinic.util.PasswordValidator;
import com.dentalclinic.util.Sanitizer;  // ADDED: Import Sanitizer
import java.util.List;

public class AccountSettingsPanel extends JPanel {
    private JTextField nameField, userField, emailField;
    private JPasswordField currentPassField, newPassField, confirmPassField;
    private JButton saveBtn;
    private int adminId;
    private String adminRole;
    private StaffDAO staffDAO = new StaffDAO();

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(210, 215, 220);
    private final Color HIGHLIGHT = new java.awt.Color(255, 253, 230);

    public AccountSettingsPanel(int id, String role, String name, String username, String email) {
        this.adminId = id;
        this.adminRole = role;

        setLayout(new GridBagLayout());
        setBackground(BG);

        // --- THE SETTINGS CARD ---
        JPanel container = new JPanel(new BorderLayout(0, 25));
        container.setPreferredSize(new Dimension(850, 500));
        container.setBackground(CARD);
        container.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(35, 45, 35, 45)
        ));

        // HEADER
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(CARD);
        JLabel title = new JLabel("My Account Settings");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("Update your personal profile and security credentials.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        
        header.add(title);
        header.add(subtitle);
        container.add(header, BorderLayout.NORTH);

        // --- TWO-COLUMN FORM GRID ---
        JPanel formGrid = new JPanel(new GridLayout(1, 2, 40, 0));
        formGrid.setBackground(CARD);

        // Column 1: Profile Information
        JPanel leftCol = new JPanel(new GridLayout(0, 1, 0, 15));
        leftCol.setBackground(CARD);
        leftCol.add(createFieldGroup("Full Name", nameField = new JTextField(name)));
        leftCol.add(createFieldGroup("Username", userField = new JTextField(username)));
        leftCol.add(createFieldGroup("Email Address", emailField = new JTextField(email)));
        formGrid.add(leftCol);

        // Column 2: Security
        JPanel rightCol = new JPanel(new GridLayout(0, 1, 0, 15));
        rightCol.setBackground(CARD);
        
        rightCol.add(createFieldGroup("New Password (Leave blank to keep current)", newPassField = new JPasswordField()));
        rightCol.add(createFieldGroup("Confirm New Password", confirmPassField = new JPasswordField()));
        
        // Verification Field (Highlighted)
        JPanel verifyGroup = createFieldGroup("CURRENT PASSWORD (REQUIRED TO SAVE)", currentPassField = new JPasswordField());
        currentPassField.setBackground(HIGHLIGHT);
        JLabel verifyLabel = (JLabel) verifyGroup.getComponent(0);
        verifyLabel.setForeground(new Color(192, 57, 43));
        rightCol.add(verifyGroup);
        
        formGrid.add(rightCol);
        container.add(formGrid, BorderLayout.CENTER);

        // --- FOOTER BUTTON ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(CARD);

        saveBtn = new JButton("Save Profile Changes");
        styleButton(saveBtn, SUCCESS);
        saveBtn.setPreferredSize(new Dimension(220, 45));
        saveBtn.addActionListener(e -> handleUpdate());

        footer.add(saveBtn);
        container.add(footer, BorderLayout.SOUTH);

        add(container);
    }

    // REMOVED: Old sanitizeInput() method - replaced with Sanitizer utility
    // private String sanitizeInput(String input) { ... }  // DELETED
    
    // SECURITY: Validate email (kept as-is, but will also use Sanitizer)
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    // --- UI HELPERS ---

    private JPanel createFieldGroup(String labelText, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(CARD);
        
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT);
        
        p.add(lbl, BorderLayout.NORTH);
        styleInputField(field);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void styleInputField(JTextField field) {
        field.setPreferredSize(new Dimension(0, 38));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 25, 10, 25));
    }

    // --- LOGIC ---

    private void handleUpdate() {
        // ==========================================================
        // FIXED: Get raw inputs and apply Sanitizer
        // ==========================================================
        String rawName = nameField.getText().trim();
        String rawUser = userField.getText().trim();
        String rawEmail = emailField.getText().trim();
        String newPw = new String(newPassField.getPassword()).trim();
        String confPw = new String(confirmPassField.getPassword()).trim();
        String currPw = new String(currentPassField.getPassword()).trim();

        // APPLY SANITIZER to all text fields
        String name = Sanitizer.sanitizeName(rawName);
        String user = Sanitizer.sanitizeUsername(rawUser);
        String email = Sanitizer.sanitizeEmail(rawEmail);

        // Validate required fields
        if (name.isEmpty() || user.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and username are required.");
            return;
        }
        
        // Validate email (using Sanitizer - returns empty if invalid)
        if (!rawEmail.isEmpty() && email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.");
            return;
        }
        
        // Validate username format
        if (!Sanitizer.isValidUsername(user)) {
            JOptionPane.showMessageDialog(this, "Username must be 3-50 characters (letters, numbers, _, ., -).");
            return;
        }
        
        // Validate current password is provided
        if (currPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter current password to verify identity.");
            return;
        }
        
        // Validate new password match
        if (!newPw.isEmpty() && !newPw.equals(confPw)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match!");
            return;
        }
        
        // Validate new password complexity
        if (!newPw.isEmpty()) {
            List<String> passwordErrors = PasswordValidator.validatePassword(newPw);
            if (!passwordErrors.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
                for (String error : passwordErrors) {
                    errorMsg.append("• ").append(error).append("\n");
                }
                JOptionPane.showMessageDialog(this, errorMsg.toString(), "Invalid Password", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            if (staffDAO.verifyPassword(adminId, currPw)) {
                if (staffDAO.updateSelf(adminId, name, user, email, newPw, adminRole)) {
                    JOptionPane.showMessageDialog(this, "Profile Updated!");
                    currentPassField.setText("");
                    newPassField.setText("");
                    confirmPassField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, "Update failed. Username may already exist.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Verification failed: Current password incorrect.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}