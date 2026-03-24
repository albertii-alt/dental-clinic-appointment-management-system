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

    public EditUserDialog(Frame parent, int id, String name, String user, String email, String role, 
                              boolean isTargetSuper, boolean amISuper) {
            super(parent, "Edit Staff Member", true);
            this.userId = id;
            this.isTargetSuperAdmin = isTargetSuper;
            this.iAmSuperAdmin = amISuper;
        
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Initialize and Set Current Data
        nameField = new JTextField(name, 20);
        userField = new JTextField(user, 20);
        passField = new JPasswordField(20);
        passField.setToolTipText("Leave blank to keep the current password");
        emailField = new JTextField(email, 20);
        roleCombo = new JComboBox<>(new String[]{"Admin", "Dentist", "Staff"});
        roleCombo.setSelectedItem(role);

        // UI Layout
        addLabelAndField("Full Name:", nameField, 0, gbc);
        addLabelAndField("Username:", userField, 1, gbc);
        addLabelAndField("New Password:", passField, 2, gbc);
        addLabelAndField("Email:", emailField, 3, gbc);
        addLabelAndField("Role:", roleCombo, 4, gbc);

        // Buttons
        updateBtn = new JButton("Update Changes");
        cancelBtn = new JButton("Cancel");

        updateBtn.addActionListener(e -> handleUpdate());
        cancelBtn.addActionListener(e -> dispose());

        JPanel bp = new JPanel();
        bp.add(updateBtn);
        bp.add(cancelBtn);
        bp.setBackground(Color.WHITE); // Keep it clean
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
        // Rule: Only a Super Admin can edit another Super Admin's core details.
        // If a Sub-Admin somehow opens this for a Super Admin, we lock the fields.
        if (isTargetSuperAdmin && !iAmSuperAdmin) {
            nameField.setEditable(false);
            userField.setEditable(false);
            emailField.setEditable(false);
            passField.setEnabled(false);
            roleCombo.setEnabled(false);
            updateBtn.setEnabled(false);
            updateBtn.setText("Locked (Super Admin Only)");
        }
        
        // Rule: Even a Super Admin shouldn't demote themselves accidentally
        // (Optional: you can disable roleCombo if userId == currentAdminId)
    }

    private void handleUpdate() {
       try {
           String name = nameField.getText().trim();
           String user = userField.getText().trim();
           String email = emailField.getText().trim();
           String role = (String) roleCombo.getSelectedItem();
           String newPass = new String(passField.getPassword()).trim();

           // Call the updated DAO method
           if (staffDAO.updateStaff(userId, name, user, email, role, newPass)) {
               result = true;
               dispose();
           }
       } catch (Exception ex) {
           JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
       }
   }

    public boolean isUpdated() { return result; }
}