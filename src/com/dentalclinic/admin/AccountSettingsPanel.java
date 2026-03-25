package com.dentalclinic.admin;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.dao.StaffDAO;

public class AccountSettingsPanel extends JPanel {
    private JTextField nameField, userField, emailField;
    private JPasswordField currentPassField, newPassField, confirmPassField;
    private JButton saveBtn;
    private int adminId;
    private String adminRole;
    private StaffDAO staffDAO = new StaffDAO();

    public AccountSettingsPanel(int id, String role, String name, String username, String email) {
        this.adminId = id;
        this.adminRole = role;

        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("My Account Settings"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fields
        nameField = new JTextField(name, 20);
        userField = new JTextField(username, 20);
        emailField = new JTextField(email, 20);
        newPassField = new JPasswordField(20);
        confirmPassField = new JPasswordField(20);
        currentPassField = new JPasswordField(20);
        currentPassField.setBackground(new Color(255, 250, 200)); // Highlight verification field

        // Build UI
        int row = 0;
        addComp("Full Name:", nameField, row++, gbc);
        addComp("Username:", userField, row++, gbc);
        addComp("Email:", emailField, row++, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        addComp("New Password:", newPassField, row++, gbc);
        addComp("Confirm New Pass:", confirmPassField, row++, gbc);
        
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        add(new JLabel("VERIFY CURRENT PASSWORD TO SAVE:"), gbc);
        gbc.gridwidth = 1;
        
        addComp("Current Password:", currentPassField, row++, gbc);

        saveBtn = new JButton("Save Changes");
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> handleUpdate());
        
        gbc.gridx = 1; gbc.gridy = row;
        add(saveBtn, gbc);
    }

    private void addComp(String label, Component c, int y, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = y;
        add(new JLabel(label), gbc);
        gbc.gridx = 1;
        add(c, gbc);
    }

    private void handleUpdate() {
        String name = nameField.getText().trim();
        String user = userField.getText().trim();
        String email = emailField.getText().trim();
        String newPw = new String(newPassField.getPassword()).trim();
        String confPw = new String(confirmPassField.getPassword()).trim();
        String currPw = new String(currentPassField.getPassword()).trim();

        if (currPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter current password to verify identity.");
            return;
        }
        if (!newPw.isEmpty() && !newPw.equals(confPw)) {
            JOptionPane.showMessageDialog(this, "New passwords do not match!");
            return;
        }

        try {
            if (staffDAO.verifyPassword(adminId, currPw)) {
                if (staffDAO.updateSelf(adminId, name, user, email, newPw, adminRole)) {
                    JOptionPane.showMessageDialog(this, "Profile Updated!");
                    currentPassField.setText("");
                    newPassField.setText("");
                    confirmPassField.setText("");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Verification failed: Current password incorrect.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}