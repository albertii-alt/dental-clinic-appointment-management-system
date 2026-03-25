package com.dentalclinic.admin;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.dao.StaffDAO;

public class EditUserDialog extends JDialog {
    private JTextField nameField, userField, emailField;
    private JComboBox<String> roleCombo;
    private JButton updateBtn, cancelBtn;
    private StaffDAO staffDAO = new StaffDAO();
    private int userId;
    private boolean result = false;
    private JPasswordField passField;
    private boolean isTargetSuperAdmin;
    private boolean iAmSuperAdmin;  
    
    // NEW FIELDS for Audit Trail
    private int adminId;
    private String adminRole;

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

        updateBtn = new JButton("Update Changes");
        cancelBtn = new JButton("Cancel");

        updateBtn.addActionListener(e -> handleUpdate());
        cancelBtn.addActionListener(e -> dispose());

        JPanel bp = new JPanel();
        bp.setBackground(Color.WHITE);
        bp.add(updateBtn);
        bp.add(cancelBtn);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        add(bp, gbc);
        
        applySecurityRestrictions();
        pack();
        setLocationRelativeTo(parent);
    }

    private void addLabelAndField(String label, Component comp, int y, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1;
        add(new JLabel(label), gbc);
        gbc.gridx = 1;
        add(comp, gbc);
    }
    
    private void applySecurityRestrictions() {
       // 1. Existing Super Admin Protection
       if (isTargetSuperAdmin && !iAmSuperAdmin) {
           nameField.setEditable(false);
           userField.setEditable(false);
           emailField.setEditable(false);
           passField.setEnabled(false);
           roleCombo.setEnabled(false);
           updateBtn.setEnabled(false);
           updateBtn.setText("Locked (Super Admin Only)");
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

           // Add a helpful tooltip to the fields
           String hint = "To edit your own account, please go to 'My Account Settings' on the sidebar.";
           nameField.setToolTipText(hint);
           userField.setToolTipText(hint);
           emailField.setToolTipText(hint);
       }
   }

    private void handleUpdate() {
       try {
           String name = nameField.getText().trim();
           String user = userField.getText().trim();
           String email = emailField.getText().trim();
           String role = (String) roleCombo.getSelectedItem();
           String newPass = new String(passField.getPassword()).trim();

           // UPDATED: Now passing adminId and adminRole for the Audit Trail
           if (staffDAO.updateStaff(userId, name, user, email, role, newPass, adminId, adminRole)) {
               result = true;
               dispose();
           }
       } catch (Exception ex) {
           JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
       }
   }

    public boolean isUpdated() { return result; }
}