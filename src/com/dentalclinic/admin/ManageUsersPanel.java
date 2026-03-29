package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.dao.StaffDAO;
import com.dentalclinic.util.PasswordValidator;

public class ManageUsersPanel extends JPanel {
    private JTextField nameField, userField, emailField;
    private JPasswordField passField;
    private JComboBox<String> roleCombo;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton saveBtn, clearBtn;
    private int selectedUserId = -1;
    private int currentAdminId;
    private boolean iAmSuperAdmin;
    private StaffDAO staffDAO = new StaffDAO();

    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private final Color DANGER_RED = new Color(231, 76, 60);
    private final Color COLUMN_SHADE = new Color(242, 245, 249);
    private final Color NAME_TEXT_COLOR = new Color(41, 128, 185);
    private final Color BG_LIGHT = new Color(245, 247, 250);

    public ManageUsersPanel(int adminId, boolean isSuper) {
        this.currentAdminId = adminId;
        this.iAmSuperAdmin = isSuper;
        
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        add(createInputForm(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        
        setupEventListeners();
        refreshTable();
    }

    private JPanel createInputForm() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(230, 230, 230), 1),
            new EmptyBorder(20, 25, 20, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 13);

        // Fields
        nameField = new JTextField(15);
        userField = new JTextField(15);
        emailField = new JTextField(15);
        passField = new JPasswordField(15);

        // SECURITY FIX: Non-Super Admin cannot assign Admin role
        if (iAmSuperAdmin) {
            roleCombo = new JComboBox<>(new String[]{"Admin", "Dentist", "Staff"});
        } else {
            roleCombo = new JComboBox<>(new String[]{"Dentist", "Staff"});
        }

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; form.add(createLabel("Full Name", labelFont), gbc);
        gbc.gridx = 1; form.add(nameField, gbc);
        gbc.gridx = 2; form.add(createLabel("Email Address", labelFont), gbc);
        gbc.gridx = 3; form.add(emailField, gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; form.add(createLabel("Username", labelFont), gbc);
        gbc.gridx = 1; form.add(userField, gbc);
        gbc.gridx = 2; form.add(createLabel("Password", labelFont), gbc);
        gbc.gridx = 3; form.add(passField, gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; form.add(createLabel("System Role", labelFont), gbc);
        gbc.gridx = 1; form.add(roleCombo, gbc);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        clearBtn = new JButton("Clear Fields");
        styleButton(clearBtn, new Color(149, 165, 166));
        clearBtn.addActionListener(e -> resetForm());

        saveBtn = new JButton("Save User Account");
        styleButton(saveBtn, PRIMARY_BLUE);
        saveBtn.addActionListener(e -> handleSaveAction());

        btnPanel.add(clearBtn);
        btnPanel.add(saveBtn);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        form.add(btnPanel, gbc);

        container.add(form, BorderLayout.CENTER);
        return container;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusable(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JLabel createLabel(String text, Font font) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(new Color(52, 73, 94));
        return l;
    }

    private JPanel createTableArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        String[] columns = {"ID", "Full Name", "Username", "Email", "Role", "Status", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        userTable = new JTable(tableModel);
        userTable.setRowHeight(45);
        userTable.setShowVerticalLines(false);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userTable.setSelectionBackground(new Color(232, 241, 249));

        // Column Renderers
        setupTableRenderers();

        JScrollPane scroll = new JScrollPane(userTable);
        scroll.setBorder(new LineBorder(new Color(230, 230, 230)));
        scroll.getViewport().setBackground(Color.WHITE);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void setupTableRenderers() {
        // --- 1. USER NAME COLUMN (Shaded with Blue Text) ---
        userTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(COLUMN_SHADE);
                    c.setForeground(NAME_TEXT_COLOR);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        // --- 2. ROLE COLUMN (Super Admin Badge) ---
        userTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                boolean isSuper = (boolean) tableModel.getValueAt(row, 6);
                if (isSuper) {
                    setText(value.toString() + " ★");
                    setForeground(new Color(211, 84, 0));
                } else {
                    setForeground(Color.BLACK);
                }
                return c;
            }
        });

        // --- 3. STATUS COLUMN (Green/Red) ---
        userTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null) {
                    if (value.toString().equalsIgnoreCase("Active")) {
                        setForeground(SUCCESS_GREEN);
                    } else {
                        setForeground(DANGER_RED);
                    }
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        // --- 4. ACTION BUTTONS (Super Admin Only) ---
        if (iAmSuperAdmin) {
            userTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    JButton deleteBtn = new JButton("Delete");
                    deleteBtn.setBackground(DANGER_RED);
                    deleteBtn.setForeground(Color.WHITE);
                    deleteBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    deleteBtn.setBorder(new EmptyBorder(5, 10, 5, 10));
                    return deleteBtn;
                }
            });
        } else {
            userTable.getColumnModel().getColumn(6).setMinWidth(0);
            userTable.getColumnModel().getColumn(6).setMaxWidth(0);
        }
    }

    private void handleSaveAction() {
        String adminRoleStr = iAmSuperAdmin ? "Super Admin" : "Admin";
        String name = nameField.getText().trim();
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());
        String email = emailField.getText().trim();
        String role = (String) roleCombo.getSelectedItem();

        // Validate email format
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address!");
            return;
        }

        if (name.isEmpty() || user.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields!");
            return;
        }

        try {
            if (selectedUserId == -1) {
                // NEW USER - Validate password complexity
                if (pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Password is required for new accounts!");
                    return;
                }
                
                // Check password complexity
                List<String> passwordErrors = PasswordValidator.validatePassword(pass);
                if (!passwordErrors.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Password requirements not met:\n");
                    for (String error : passwordErrors) {
                        errorMsg.append("• ").append(error).append("\n");
                    }
                    JOptionPane.showMessageDialog(this, errorMsg.toString(), "Invalid Password", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (staffDAO.addStaff(name, user, pass, email, role, currentAdminId, adminRoleStr)) {
                    JOptionPane.showMessageDialog(this, "Staff added successfully!");
                }
            } else {
                // UPDATE USER - Handle password if provided
                String passwordToUpdate = pass.isEmpty() ? null : pass;
                
                // If password is provided, validate complexity
                if (passwordToUpdate != null) {
                    List<String> passwordErrors = PasswordValidator.validatePassword(passwordToUpdate);
                    if (!passwordErrors.isEmpty()) {
                        StringBuilder errorMsg = new StringBuilder("New password requirements not met:\n");
                        for (String error : passwordErrors) {
                            errorMsg.append("• ").append(error).append("\n");
                        }
                        JOptionPane.showMessageDialog(this, errorMsg.toString(), "Invalid Password", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                if (staffDAO.updateStaff(selectedUserId, name, user, email, role, passwordToUpdate, currentAdminId, adminRoleStr)) {
                    JOptionPane.showMessageDialog(this, "Staff updated successfully!");
                }
            }
            resetForm();
            refreshTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void setupEventListeners() {
        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = userTable.rowAtPoint(e.getPoint());
                int column = userTable.columnAtPoint(e.getPoint());
                if (row == -1) return;

                int id = (int) tableModel.getValueAt(row, 0);
                String name = (String) tableModel.getValueAt(row, 1);
                boolean targetIsSuper = (boolean) tableModel.getValueAt(row, 6);
                String adminRoleStr = iAmSuperAdmin ? "Super Admin" : "Admin";

                // DELETE LOGIC
                if (column == 6 && iAmSuperAdmin) {
                    handleDelete(id, name, adminRoleStr);
                } 
                // STATUS TOGGLE LOGIC
                else if (column == 5) {
                    handleToggle(id, name, targetIsSuper, (String)tableModel.getValueAt(row, 5), adminRoleStr);
                }
                // EDIT LOGIC (Double Click)
                else if (e.getClickCount() == 2) {
                    handleEdit(row, id, name, targetIsSuper, adminRoleStr);
                }
            }
        });
    }

    // --- LOGIC METHODS ---
    private void handleDelete(int id, String name, String adminRole) {
        if (id == currentAdminId) {
            JOptionPane.showMessageDialog(this, "Security Error: You cannot delete your own account.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Permanently delete " + name + "?", "Warning", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try { if (staffDAO.deleteStaff(id, name, currentAdminId, adminRole)) refreshTable(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void handleToggle(int id, String name, boolean targetIsSuper, String currentStatus, String adminRole) {
        if (id == currentAdminId) {
            JOptionPane.showMessageDialog(null, "Security Restriction: You cannot deactivate yourself.");
            return;
        }
        if (targetIsSuper && !iAmSuperAdmin) {
            JOptionPane.showMessageDialog(null, "Access Denied: Super Admin privilege required.");
            return;
        }
        boolean isActive = currentStatus.equalsIgnoreCase("Active");
        int confirm = JOptionPane.showConfirmDialog(this, "Toggle status for " + name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try { if (staffDAO.toggleStaffStatus(id, isActive, currentAdminId, adminRole)) refreshTable(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void handleEdit(int row, int id, String name, boolean targetIsSuper, String adminRole) {
        String user = (String) tableModel.getValueAt(row, 2);
        String email = (String) tableModel.getValueAt(row, 3);
        String role = (String) tableModel.getValueAt(row, 4);
        EditUserDialog dialog = new EditUserDialog((Frame)SwingUtilities.windowForComponent(this), 
            id, name, user, email, role, targetIsSuper, iAmSuperAdmin, currentAdminId, adminRole);
        dialog.setVisible(true);
        if (dialog.isUpdated()) refreshTable();
    }

    public void refreshTable() {
        try {
            tableModel.setRowCount(0);
            List<Object[]> staffList = staffDAO.getAllStaff();
            for (Object[] staff : staffList) tableModel.addRow(staff);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void resetForm() {
        nameField.setText(""); 
        userField.setText("");
        emailField.setText(""); 
        passField.setText("");
        roleCombo.setSelectedIndex(0);
        selectedUserId = -1;
        saveBtn.setText("Save User Account");
        saveBtn.setBackground(PRIMARY_BLUE);
    }
        public void cleanup() {
        System.out.println("Cleaning up ManageUsersPanel...");
        // Clear table model to release references
        if (tableModel != null) {
            tableModel.setRowCount(0);
        }
        // Clear any pending operations
        // The DAO connections will be closed automatically
    }
}