package com.dentalclinic.view.admin;

import com.dentalclinic.controller.LogController;
import com.dentalclinic.controller.RolesController;
import com.dentalclinic.model.Permission;
import com.dentalclinic.model.Role;
import com.dentalclinic.util.UserSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ManageRolesPanel extends JPanel {

    private static final long CACHE_TTL_MS = 30000;

    private static RolesInitData rolesInitCache;
    private static long rolesInitCacheAtMs = 0;
    private static final Map<Integer, RolePermissionCacheEntry> ROLE_PERMISSION_CACHE = new ConcurrentHashMap<>();

    private RolesController rolesController;
    private final LogController logController = new LogController();
    private JList<String> roleList;
    private JPanel permissionsContainer;
    private Map<Integer, JCheckBox> checkBoxes;
    private List<Permission> allPermissions;
    private Map<String, Integer> roleIdMap;
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String adminRole;
    private SwingWorker<RolesInitData, Void> rolesInitWorker;
    private SwingWorker<List<Integer>, Void> rolePermissionWorker;
    private long rolePermissionRequestId = 0;

    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private final Color BG_LIGHT = new Color(245, 247, 250);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_MUTED = new Color(127, 140, 141);

    private static class RolesInitData {
        private final Map<String, Integer> roleMap;
        private final List<Permission> permissions;

        private RolesInitData(Map<String, Integer> roleMap, List<Permission> permissions) {
            this.roleMap = roleMap;
            this.permissions = permissions;
        }
    }

    private static class RolePermissionCacheEntry {
        private final List<Integer> permissionIds;
        private final long createdAtMs;

        private RolePermissionCacheEntry(List<Integer> permissionIds, long createdAtMs) {
            this.permissionIds = permissionIds;
            this.createdAtMs = createdAtMs;
        }
    }

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
        roleIdMap = new HashMap<>();

        setLayout(new BorderLayout(25, 25));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(30, 40, 30, 40));

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

        add(createSplitContent(), BorderLayout.CENTER);
        add(createFooterActions(), BorderLayout.SOUTH);

        loadRolesAndPermissionsAsync(false);
    }

    private RolesInitData loadRolesAndPermissionsFromDatabase() {
        Map<String, Integer> roleMap = new HashMap<>();
        List<Permission> permissions = new ArrayList<>();

        try {
            List<Role> roles = rolesController.getAllRoles();
            for (Role role : roles) {
                roleMap.put(role.getRoleName(), role.getRoleId());
            }
            permissions = rolesController.getAllPermissions();
        } catch (Exception e) {
            e.printStackTrace();
            roleMap.put("Admin", 1);
            roleMap.put("Dentist", 2);
            roleMap.put("Staff", 3);
        }

        return new RolesInitData(roleMap, permissions);
    }

    private JSplitPane createSplitContent() {
        roleList = new JList<>(new String[0]);
        roleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roleList.setFixedCellHeight(50);
        roleList.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roleList.setBackground(Color.WHITE);
        roleList.setSelectionBackground(new Color(232, 241, 249));
        roleList.setSelectionForeground(PRIMARY_BLUE);
        roleList.setBorder(new EmptyBorder(10, 10, 10, 10));

        roleList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadPermissionsForSelectedRole(false);
            }
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

        permissionsContainer = new JPanel();
        permissionsContainer.setLayout(new BoxLayout(permissionsContainer, BoxLayout.Y_AXIS));
        permissionsContainer.setBackground(Color.WHITE);
        permissionsContainer.setBorder(new EmptyBorder(20, 25, 20, 25));

        loadAllPermissionCheckboxes(Collections.emptyList());

        JScrollPane scrollPane = new JScrollPane(permissionsContainer);
        scrollPane.setBorder(new LineBorder(new Color(230, 230, 230)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(Color.WHITE);

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

    private void loadAllPermissionCheckboxes(List<Permission> permissions) {
        permissionsContainer.removeAll();
        checkBoxes.clear();
        allPermissions = permissions;

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

            JCheckBox cb = new JCheckBox(p.getPermissionName());
            cb.setBackground(Color.WHITE);
            cb.setFont(new Font("Segoe UI", Font.BOLD, 14));
            cb.setForeground(TEXT_DARK);
            cb.setFocusable(false);
            itemPanel.add(cb, gbc);

            gbc.gridy = 1;
            gbc.insets = new Insets(2, 28, 0, 0);

            JLabel descLabel = new JLabel("<html><body style='width: 100%'>" + p.getDescription() + "</body></html>");
            descLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            descLabel.setForeground(TEXT_MUTED);
            itemPanel.add(descLabel, gbc);

            checkBoxes.put(p.getPermissionId(), cb);
            permissionsContainer.add(itemPanel);

            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(240, 240, 240));
            permissionsContainer.add(sep);
        }

        permissionsContainer.revalidate();
        permissionsContainer.repaint();
    }

    private void loadRolesAndPermissionsAsync(boolean forceRefresh) {
        if (!forceRefresh && rolesInitCache != null && System.currentTimeMillis() - rolesInitCacheAtMs <= CACHE_TTL_MS) {
            applyRolesInitData(rolesInitCache);
            return;
        }

        if (rolesInitWorker != null && !rolesInitWorker.isDone()) {
            rolesInitWorker.cancel(true);
        }

        roleList.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        rolesInitWorker = new SwingWorker<RolesInitData, Void>() {
            @Override
            protected RolesInitData doInBackground() {
                return loadRolesAndPermissionsFromDatabase();
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    RolesInitData data = get();
                    rolesInitCache = data;
                    rolesInitCacheAtMs = System.currentTimeMillis();
                    applyRolesInitData(data);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ManageRolesPanel.this,
                            "Error loading roles and permissions: " + e.getMessage(),
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    roleList.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        rolesInitWorker.execute();
    }

    private void applyRolesInitData(RolesInitData data) {
        roleIdMap.clear();
        roleIdMap.putAll(data.roleMap);

        loadAllPermissionCheckboxes(data.permissions != null ? data.permissions : Collections.emptyList());

        String[] roles = roleIdMap.keySet().toArray(new String[0]);
        roleList.setListData(roles);
        if (roles.length > 0) {
            roleList.setSelectedIndex(0);
            loadPermissionsForSelectedRole(false);
        }
    }

    private void loadPermissionsForSelectedRole(boolean forceRefresh) {
        String selectedRole = roleList.getSelectedValue();
        if (selectedRole == null) {
            return;
        }

        Integer roleId = roleIdMap.get(selectedRole);
        if (roleId == null) {
            return;
        }

        checkBoxes.values().forEach(cb -> cb.setSelected(false));

        RolePermissionCacheEntry cached = ROLE_PERMISSION_CACHE.get(roleId);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            applyRolePermissionSelection(cached.permissionIds);
            return;
        }

        if (rolePermissionWorker != null && !rolePermissionWorker.isDone()) {
            rolePermissionWorker.cancel(true);
        }

        final long requestId = ++rolePermissionRequestId;
        roleList.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        rolePermissionWorker = new SwingWorker<List<Integer>, Void>() {
            @Override
            protected List<Integer> doInBackground() {
                return rolesController.getPermissionIdsForRole(roleId);
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != rolePermissionRequestId) {
                    return;
                }

                try {
                    List<Integer> activePerms = get();
                    ROLE_PERMISSION_CACHE.put(roleId,
                            new RolePermissionCacheEntry(new ArrayList<>(activePerms), System.currentTimeMillis()));
                    applyRolePermissionSelection(activePerms);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ManageRolesPanel.this,
                            "Error loading role permissions: " + e.getMessage(),
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    roleList.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        rolePermissionWorker.execute();
    }

    private void applyRolePermissionSelection(List<Integer> activePerms) {
        checkBoxes.values().forEach(cb -> cb.setSelected(false));
        for (Integer permId : activePerms) {
            JCheckBox cb = checkBoxes.get(permId);
            if (cb != null) {
                cb.setSelected(true);
            }
        }
    }

    private void savePermissions() {
        String selectedRole = roleList.getSelectedValue();
        if (selectedRole == null) {
            JOptionPane.showMessageDialog(this, "Please select a role first.", "No Role Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer roleId = roleIdMap.get(selectedRole);
        if (roleId == null) {
            JOptionPane.showMessageDialog(this, "Error: Role not found.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Integer> selectedIds = new ArrayList<>();
        checkBoxes.forEach((id, cb) -> {
            if (cb.isSelected()) {
                selectedIds.add(id);
            }
        });

        if (rolesController.saveRolePermissions(roleId, selectedIds)) {
            ROLE_PERMISSION_CACHE.put(roleId,
                    new RolePermissionCacheEntry(new ArrayList<>(selectedIds), System.currentTimeMillis()));

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
