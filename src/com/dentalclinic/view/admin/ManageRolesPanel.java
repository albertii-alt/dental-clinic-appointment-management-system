package com.dentalclinic.view.admin;

import com.dentalclinic.controller.RolesController;
import com.dentalclinic.controller.LogController;
import com.dentalclinic.model.Permission;
import com.dentalclinic.model.Role;
import com.dentalclinic.util.UserSession;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageRolesPanel extends JPanel {

    private RolesController rolesController;
    private final LogController logController = new LogController();
    private JList<String> roleList;
    private JPanel permissionsContainer;
    private Map<Integer, JCheckBox> checkBoxes; 
    private List<Permission> allPermissions;
    private Map<String, Integer> roleIdMap; // Dynamically loaded
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String adminRole;
    
    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private final Color BG_LIGHT = new Color(245, 247, 250);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_MUTED = new Color(127, 140, 141);

    public ManageRolesPanel(int adminId, boolean isSuper) {
        this.currentAdminId = adminId;
        this.isSuperAdmin = isSuper;
        this.adminRole = isSuper ? "Super Admin" : "Admin";
        rolesController = new RolesController();
        if (!rolesController.canCurrentUserManageRoles()) {
            JOptionPane.showMessageDialog(null, 
                "Access Denied: You do not have permission to manage roles.", 
                "Security Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        checkBoxes = new HashMap<>();
        roleIdMap = loadRoleIdsFromDatabase();
        
        setLayout(new BorderLayout(25, 25));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- HEADER SECTION ---
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);
        
        JLabel title = new JLabel("Roles & Permissions Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_DARK);
        
        JLabel subtitle = new JLabel("Configure system access levels for each staff category");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_MUTED);
        
        headerPanel.add(title);
        headerPanel.add(subtitle);
        add(headerPanel, BorderLayout.NORTH);

        // --- CENTER SPLIT SECTION ---
        add(createSplitContent(), BorderLayout.CENTER);

        // --- BOTTOM ACTION SECTION ---
        add(createFooterActions(), BorderLayout.SOUTH);

        // Initial Load
        loadPermissionsForSelectedRole();
    }

    /**
     * SECURITY FIX: Load role IDs dynamically from database instead of hardcoding
     */
    private Map<String, Integer> loadRoleIdsFromDatabase() {
        Map<String, Integer> roleMap = new HashMap<>();
        try {
            List<Role> roles = rolesController.getAllRoles();
            for (Role role : roles) {
                roleMap.put(role.getRoleName(), role.getRoleId());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to hardcoded values if database query fails
            roleMap.put("Admin", 1);
            roleMap.put("Dentist", 2);
            roleMap.put("Staff", 3);
        }
        return roleMap;
    }

    private JSplitPane createSplitContent() {
        // --- LEFT SIDE: ROLE SELECTOR ---
        String[] roles = roleIdMap.keySet().toArray(new String[0]);
        roleList = new JList<>(roles);
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (roles.length > 0) roleList.setSelectedIndex(0);
        roleList.setFixedCellHeight(50);
        roleList.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleList.setBackground(Color.WHITE);
        roleList.setSelectionBackground(new Color(232, 241, 249));
        roleList.setSelectionForeground(PRIMARY_BLUE);
        roleList.setBorder(new EmptyBorder(10, 10, 10, 10));

        roleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadPermissionsForSelectedRole();
        });

        JPanel roleWrapper = new JPanel(new BorderLayout());
        roleWrapper.setBackground(Color.WHITE);
        roleWrapper.setBorder(new LineBorder(new Color(230, 230, 230)));
        JLabel roleLabel = new JLabel(" SYSTEM ROLES", SwingConstants.LEFT);
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setForeground(TEXT_MUTED);
        roleLabel.setBorder(new EmptyBorder(15, 15, 10, 15));
        roleWrapper.add(roleLabel, BorderLayout.NORTH);
        roleWrapper.add(roleList, BorderLayout.CENTER);

        // --- RIGHT SIDE: PERMISSIONS LIST ---
        permissionsContainer = new JPanel();
        permissionsContainer.setLayout(new BoxLayout(permissionsContainer, BoxLayout.Y_AXIS));
        permissionsContainer.setBackground(Color.WHITE);
        permissionsContainer.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        loadAllPermissionCheckboxes();

        JScrollPane scrollPane = new JScrollPane(permissionsContainer);
        scrollPane.setBorder(new LineBorder(new Color(230, 230, 230)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Color.WHITE);

        // Split Pane Customization
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, roleWrapper, scrollPane);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(10);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        
        return splitPane;
    }

    private JPanel createFooterActions() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);

        JButton saveBtn = new JButton("Save Permissions for Selected Role");
        saveBtn.setBackground(SUCCESS_GREEN);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusable(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        saveBtn.setBorder(new EmptyBorder(12, 25, 12, 25));
        saveBtn.addActionListener(e -> savePermissions());

        footer.add(saveBtn);
        return footer;
    }

    private void loadAllPermissionCheckboxes() {
        allPermissions = rolesController.getAllPermissions();
        for (Permission p : allPermissions) {
            JPanel itemPanel = new JPanel(new GridBagLayout());
            itemPanel.setBackground(Color.WHITE);
            itemPanel.setBorder(new EmptyBorder(8, 0, 12, 0));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            // 1. The Checkbox (Permission Name)
            JCheckBox cb = new JCheckBox(p.getPermissionName());
            cb.setBackground(Color.WHITE);
            cb.setFont(new Font("Segoe UI", Font.BOLD, 14));
            cb.setForeground(TEXT_DARK);
            cb.setFocusable(false);
            itemPanel.add(cb, gbc);

            // 2. The Description (The text below)
            gbc.gridy = 1;
            gbc.insets = new Insets(2, 28, 0, 0);

            // Using HTML to allow the description to wrap
            JLabel descLabel = new JLabel("<html><body style='width: 100%'>" + p.getDescription() + "</body></html>");
            descLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            descLabel.setForeground(TEXT_MUTED);
            itemPanel.add(descLabel, gbc);

            checkBoxes.put(p.getPermissionId(), cb);
            permissionsContainer.add(itemPanel);

            // Separator line between permissions
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(240, 240, 240));
            permissionsContainer.add(sep);
        }
    }

    private void loadPermissionsForSelectedRole() {
        String selectedRole = roleList.getSelectedValue();
        if (selectedRole == null) return;
        
        Integer roleId = roleIdMap.get(selectedRole);
        if (roleId == null) return;

        // Clear all checkboxes
        checkBoxes.values().forEach(cb -> cb.setSelected(false));

        // Get permissions from DB
        List<Integer> activePerms = rolesController.getPermissionIdsForRole(roleId);
        for (Integer permId : activePerms) {
            if (checkBoxes.containsKey(permId)) {
                checkBoxes.get(permId).setSelected(true);
            }
        }
    }

    private void savePermissions() {
        String selectedRole = roleList.getSelectedValue();
        Integer roleId = roleIdMap.get(selectedRole);
        if (roleId == null) {
            JOptionPane.showMessageDialog(this, "Error: Role not found.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Integer> selectedIds = new ArrayList<>();
        checkBoxes.forEach((id, cb) -> {
            if (cb.isSelected()) selectedIds.add(id);
        });

        if (rolesController.saveRolePermissions(roleId, selectedIds)) {
            // SECURITY: Log the permission change
            int adminId = UserSession.getUserId();
            String adminRole = UserSession.getUserRole();
            String adminName = UserSession.getFullName();

            String logDetails = String.format(
                "Admin %s updated permissions for role: %s. Active permissions: %s", 
                adminName, selectedRole, selectedIds.toString()
            );

            logController.record(adminId, adminRole, "UPDATE_ROLE_PERMISSIONS", logDetails);

            JOptionPane.showMessageDialog(this, "Security profiles updated successfully for " + selectedRole);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update permissions.", "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    public void cleanup() {
    System.out.println("Cleaning up ManageRolesPanel...");
}
}
