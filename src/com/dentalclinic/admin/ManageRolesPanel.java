package com.dentalclinic.admin;

import com.dentalclinic.dao.RolesPermissionDAO;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageRolesPanel extends JPanel {

    private RolesPermissionDAO dao;
    private JList<String> roleList;
    private JPanel permissionsContainer;
    private Map<Integer, JCheckBox> checkBoxes; // Maps Permission ID to its CheckBox
    private List<RolesPermissionDAO.Permission> allPermissions;
    
    // Hardcoded roles for now based on our SQL setup
    private final String[] roles = {"Admin", "Dentist", "Staff"};
    private final Map<String, Integer> roleIdMap = Map.of("Admin", 1, "Dentist", 2, "Staff", 3);

    public ManageRolesPanel() {
        dao = new RolesPermissionDAO();
        checkBoxes = new HashMap<>();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TITLE ---
        JLabel title = new JLabel("Roles & Permissions Management");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // --- LEFT SIDE: ROLE LIST ---
        roleList = new JList<>(roles);
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleList.setSelectedIndex(0);
        roleList.setFixedCellHeight(40);
        roleList.setBorder(BorderFactory.createTitledBorder("Select Role"));
        
        roleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadPermissionsForSelectedRole();
        });

        // --- RIGHT SIDE: PERMISSIONS CHECKBOXES ---
        permissionsContainer = new JPanel();
        permissionsContainer.setLayout(new BoxLayout(permissionsContainer, BoxLayout.Y_AXIS));
        permissionsContainer.setBackground(Color.WHITE);
        
        loadAllPermissionCheckboxes();

        JScrollPane scrollPane = new JScrollPane(permissionsContainer);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Permissions"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roleList, scrollPane);
        splitPane.setDividerLocation(200);
        add(splitPane, BorderLayout.CENTER);

        // --- BOTTOM: SAVE BUTTON ---
        JButton saveBtn = new JButton("Save Permissions for Role");
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("Arial", Font.BOLD, 14));
        saveBtn.addActionListener(e -> savePermissions());
        
        add(saveBtn, BorderLayout.SOUTH);

        // Initial Load
        loadPermissionsForSelectedRole();
    }

    private void loadAllPermissionCheckboxes() {
        allPermissions = dao.getAllPermissions();
        for (RolesPermissionDAO.Permission p : allPermissions) {
            JCheckBox cb = new JCheckBox(p.name);
            cb.setToolTipText(p.description); // Show description on hover
            cb.setBackground(Color.WHITE);
            cb.setFont(new Font("Arial", Font.PLAIN, 13));
            
            checkBoxes.put(p.id, cb);
            permissionsContainer.add(cb);
            permissionsContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }

    private void loadPermissionsForSelectedRole() {
        String selectedRole = roleList.getSelectedValue();
        int roleId = roleIdMap.get(selectedRole);

        // Clear all checkboxes first
        checkBoxes.values().forEach(cb -> cb.setSelected(false));

        // Get permissions from DB
        List<Integer> activePerms = dao.getPermissionIdsForRole(roleId);
        for (Integer permId : activePerms) {
            if (checkBoxes.containsKey(permId)) {
                checkBoxes.get(permId).setSelected(true);
            }
        }
    }

    private void savePermissions() {
        String selectedRole = roleList.getSelectedValue();
        int roleId = roleIdMap.get(selectedRole);

        // 1. Get the "Before" state (optional but better for logs)
        List<Integer> oldPerms = dao.getPermissionIdsForRole(roleId);

        // 2. Get the "After" state from Checkboxes
        List<Integer> selectedIds = new ArrayList<>();
        checkBoxes.forEach((id, cb) -> {
            if (cb.isSelected()) selectedIds.add(id);
        });

        // 3. Execute the update
        if (dao.updateRolePermissions(roleId, selectedIds)) {

            // 4. RECORD THE ACTIVITY
            com.dentalclinic.service.LogService logService = new com.dentalclinic.service.LogService();

            int adminId = com.dentalclinic.util.UserSession.getUserId();
            String adminRole = com.dentalclinic.util.UserSession.getUserRole();
            String adminName = com.dentalclinic.util.UserSession.getFullName();

            String logDetails = String.format(
                "Admin %s updated permissions for role: %s. New Permission IDs: %s", 
                adminName, selectedRole, selectedIds.toString()
            );

            // Save to activity_logs table
            logService.record(adminId, adminRole, "UPDATE_ROLE_PERMISSIONS", logDetails);

            JOptionPane.showMessageDialog(this, "Permissions updated for " + selectedRole);
        } else {
            JOptionPane.showMessageDialog(this, "Error updating permissions.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}