package com.dentalclinic.view.admin;

import com.dentalclinic.controller.AdminController;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.util.PasswordValidator;

public class EditUserDialog extends JDialog {
    private JTextField nameField, userField, emailField;
    private JComboBox<String> roleCombo;
    private JButton updateBtn, cancelBtn;
    private final AdminController adminController = new AdminController();
    private int userId;
    private boolean result = false;
    private JPasswordField passField;
    private boolean isTargetSuperAdmin;
    private boolean iAmSuperAdmin;  
    
    // Audit Trail fields
    private int adminId;
    private String adminRole;
    
    // UI Colors
    private final Color DANGER_RED = new Color(231, 76, 60);
    private final Color WARNING_ORANGE = new Color(243, 156, 18);

    public EditUserDialog(Frame parent, int id, String name, String user, String email, String role, 
                          boolean isTargetSuper, boolean amISuper, int adminId, String adminRole) {
        super(parent, "Edit Staff Member", true);
        this.userId = id;
        this.isTargetSuperAdmin = isTargetSuper;
        this.iAmSuperAdmin = amISuper;
        this.adminId = adminId;
        this.adminRole = adminRole;
        
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        nameField = new JTextField(name, 20);
        userField = new JTextField(user, 20);
        passField = new JPasswordField(20);
        passField.setToolTipText("Leave blank to keep the current password");
        emailField = new JTextField(email, 20);
        roleCombo = new JComboBox<>(new String[]{"Admin", "Dentist", "Staff"});
        roleCombo.setSelectedItem(role);

        addLabelAndField("Full Name:", nameField, 0, gbc);
        addLabelAndField("Username:", userField, 1, gbc);
        addLabelAndField("New Password:", passField, 2, gbc);
        addLabelAndField("Email:", emailField, 3, gbc);
        addLabelAndField("Role:", roleCombo, 4, gbc);

        // Add password requirements hint
        JLabel passHint = new JLabel("<html><small>Password must be at least 8 characters with uppercase, lowercase, number, and special character</small></html>");
        passHint.setForeground(new Color(127, 140, 141));
        passHint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 10, 10, 10);
        add(passHint, gbc);

        updateBtn = new JButton("Update Changes");
        cancelBtn = new JButton("Cancel");

        updateBtn.addActionListener(e -> handleUpdate());
        cancelBtn.addActionListener(e -> dispose());

        JPanel bp = new JPanel();
        bp.setBackground(Color.WHITE);
        bp.add(updateBtn);
        bp.add(cancelBtn);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 10, 10);
        add(bp, gbc);
        
        applySecurityRestrictions();
        pack();
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(450, 400));
    }

    private void addLabelAndField(String label, Component comp, int y, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(44, 62, 80));
        add(lbl, gbc);
        gbc.gridx = 1;
        add(comp, gbc);
    }
    
    private void applySecurityRestrictions() {
        // 1. Super Admin Protection - Cannot edit a Super Admin unless you are also Super Admin
        if (isTargetSuperAdmin && !iAmSuperAdmin) {
            nameField.setEditable(false);
            userField.setEditable(false);
            emailField.setEditable(false);
            passField.setEnabled(false);
            roleCombo.setEnabled(false);
            updateBtn.setEnabled(false);
            updateBtn.setText("Locked (Super Admin Only)");
            updateBtn.setBackground(WARNING_ORANGE);
            
            JLabel warning = new JLabel(" This user has Super Admin privileges. Only another Super Admin can edit this account.");
            warning.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_TRIANGLE, 13, DANGER_RED));
            warning.setForeground(DANGER_RED);
            warning.setFont(new Font("Segoe UI", Font.BOLD, 11));
            // Add warning to dialog (simplified - you might want to add it properly)
        }

        // 2. SELF-EDIT PROTECTION: Lock everything if this is the logged-in admin
        if (userId == adminId) {
            nameField.setEditable(false);
            userField.setEditable(false);
            emailField.setEditable(false);
            passField.setEnabled(false);
            roleCombo.setEnabled(false);
            updateBtn.setEnabled(false);
            updateBtn.setText("Use 'Account Settings' to Edit Self");
            updateBtn.setBackground(WARNING_ORANGE);
            
            String hint = "To edit your own account, please go to 'My Account Settings' on the sidebar.";
            nameField.setToolTipText(hint);
            userField.setToolTipText(hint);
            emailField.setToolTipText(hint);
            passField.setToolTipText(hint);
        }
        
        // 3. Prevent role escalation: Non-Super Admin cannot assign Admin role to anyone
        if (!iAmSuperAdmin && roleCombo.isEnabled()) {
            // Remove Admin option from dropdown for non-Super Admins
            roleCombo.removeItem("Admin");
            // If current role was Admin, show warning
            if (roleCombo.getSelectedItem() == null || roleCombo.getSelectedItem().equals("Admin")) {
                roleCombo.setSelectedIndex(0);
            }
        }
    }

    private void handleUpdate() {
        try {
            String name = nameField.getText().trim();
            String user = userField.getText().trim();
            String email = emailField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();
            String newPass = new String(passField.getPassword()).trim();
            
            // Basic validation
            if (name.isEmpty() || user.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please fill in all required fields!", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter a valid email address!", 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // SECURITY FIX: If password is provided, validate complexity
            String passwordToUpdate = newPass.isEmpty() ? null : newPass;
            if (passwordToUpdate != null) {
                List<String> passwordErrors = PasswordValidator.validatePassword(passwordToUpdate);
                if (!passwordErrors.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
                    for (String error : passwordErrors) {
                        errorMsg.append("• ").append(error).append("\n");
                    }
                    JOptionPane.showMessageDialog(this, 
                        errorMsg.toString(), 
                        "Invalid Password", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // SECURITY FIX: Prevent role escalation to Admin for non-Super Admin
            if (!iAmSuperAdmin && "Admin".equals(role)) {
                JOptionPane.showMessageDialog(this, 
                    "Security Restriction: You cannot assign Admin role.", 
                    "Access Denied", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // SECURITY FIX: Prevent self-role change (already handled in applySecurityRestrictions)
            if (userId == adminId && !role.equals((String) roleCombo.getSelectedItem())) {
                JOptionPane.showMessageDialog(this, 
                    "You cannot change your own role. Use Account Settings instead.", 
                    "Security Restriction", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Update the staff member
            if (adminController.updateStaff(userId, name, user, email, role, passwordToUpdate, adminId, adminRole)) {
                JOptionPane.showMessageDialog(this, 
                    "Staff member updated successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                result = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to update staff member. Please try again.", 
                    "Update Failed", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error: " + ex.getMessage(), 
                "System Error", 
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public boolean isUpdated() { 
        return result; 
    }
}
