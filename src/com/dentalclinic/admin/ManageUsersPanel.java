package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Vector;

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
    private com.dentalclinic.dao.StaffDAO staffDAO = new com.dentalclinic.dao.StaffDAO();

    public ManageUsersPanel(int adminId, boolean isSuper) {
        this.currentAdminId = adminId;
        this.iAmSuperAdmin = isSuper;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(createInputForm(), BorderLayout.NORTH);
        add(createTableArea(), BorderLayout.CENTER);
        refreshTable();
        
        userTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = userTable.columnAtPoint(e.getPoint());
                int row = userTable.rowAtPoint(e.getPoint());

                if (row == -1) return; 

                // 1. Extract data safely
                int id = (int) tableModel.getValueAt(row, 0);
                String name = (String) tableModel.getValueAt(row, 1);
                boolean targetIsSuper = (boolean) tableModel.getValueAt(row, 6);

                // Define this here so it can be used in all actions below
                String adminRoleStr = iAmSuperAdmin ? "Super Admin" : "Admin";

                // --- LOGIC: PERMANENT DELETE (Column 6) ---
                if (column == 6 && iAmSuperAdmin) {
                    if (id == currentAdminId) {
                        JOptionPane.showMessageDialog(ManageUsersPanel.this, "Security Error: You cannot delete your own account.");
                        return;
                    }

                    int confirm = JOptionPane.showConfirmDialog(ManageUsersPanel.this, 
                        "CRITICAL: Delete " + name + " permanently?\nThis will erase all their records!", 
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            // Correctly passing the 4 required parameters to deleteStaff
                            if (staffDAO.deleteStaff(id, name, currentAdminId, adminRoleStr)) {
                                refreshTable();
                                JOptionPane.showMessageDialog(ManageUsersPanel.this, "User deleted from system.");
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ManageUsersPanel.this, "Database Error: " + ex.getMessage());
                        }
                    }
                    return; 
                }

                // --- LOGIC: STATUS TOGGLE (Column 5) ---
                if (column == 5) {
                    if (id == currentAdminId) {
                        JOptionPane.showMessageDialog(null, "Security Restriction: You cannot deactivate your own account.");
                        return; 
                    }
                    if (targetIsSuper && !iAmSuperAdmin) {
                        JOptionPane.showMessageDialog(null, "Access Denied: Only a Super Admin can modify another Super Admin.");
                        return;
                    }

                    String currentStatus = (String) tableModel.getValueAt(row, 5);
                    boolean isActive = currentStatus.equalsIgnoreCase("Active");
                    String action = isActive ? "Deactivate" : "Activate";

                    int confirm = JOptionPane.showConfirmDialog(ManageUsersPanel.this, "Are you sure you want to " + action + " " + name + "?", "Confirm Status Change", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            if (staffDAO.toggleStaffStatus(id, isActive, currentAdminId, adminRoleStr)) {
                                refreshTable();
                            }
                        } catch (java.sql.SQLException ex) {
                            JOptionPane.showMessageDialog(ManageUsersPanel.this, "Error: " + ex.getMessage());
                        }
                    }
                } 

                // --- LOGIC: EDIT MODAL (Double Click) ---
                else if (e.getClickCount() == 2) {
                    String user = (String) tableModel.getValueAt(row, 2);
                    String email = (String) tableModel.getValueAt(row, 3);
                    String role = (String) tableModel.getValueAt(row, 4);

                    Window parentWindow = SwingUtilities.windowForComponent(ManageUsersPanel.this);

                    // UPDATED: Now passing all 10 parameters to match the new EditUserDialog constructor
                    EditUserDialog dialog = new EditUserDialog(
                        (Frame)parentWindow, id, name, user, email, role, 
                        targetIsSuper, iAmSuperAdmin, currentAdminId, adminRoleStr
                    );

                    dialog.setVisible(true);

                    if (dialog.isUpdated()) {
                        refreshTable();
                    }
                }
            }
        });
    }

    private JPanel createInputForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Register / Edit Staff"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Labels and Fields
        nameField = new JTextField(15);
        userField = new JTextField(15);
        emailField = new JTextField(15);
        passField = new JPasswordField(15);
        roleCombo = new JComboBox<>(new String[]{"Admin", "Dentist", "Staff"});

        addComponent(form, new JLabel("Full Name:"), 0, 0, gbc);
        addComponent(form, nameField, 1, 0, gbc);
        
        addComponent(form, new JLabel("Username:"), 0, 1, gbc);
        addComponent(form, userField, 1, 1, gbc);

        addComponent(form, new JLabel("Email:"), 2, 0, gbc);
        addComponent(form, emailField, 3, 0, gbc);

        addComponent(form, new JLabel("Password:"), 2, 1, gbc);
        addComponent(form, passField, 3, 1, gbc);

        addComponent(form, new JLabel("Role:"), 0, 2, gbc);
        addComponent(form, roleCombo, 1, 2, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(Color.WHITE);
        saveBtn = new JButton("Save User");
        saveBtn.addActionListener(e -> {
            String adminRoleStr = iAmSuperAdmin ? "Super Admin" : "Admin";
            String name = nameField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            String email = emailField.getText().trim();
            String role = (String) roleCombo.getSelectedItem();

            if (name.isEmpty() || user.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all required fields!");
                return;
            }

            try {
                if (selectedUserId == -1) {
                    // ADD MODE - Pass the 2 extra audit parameters
                    if (staffDAO.addStaff(name, user, pass, email, role, currentAdminId, adminRoleStr)) {
                        JOptionPane.showMessageDialog(this, "Staff added and logged!");
                    }
                } else {
                    // UPDATE MODE - Pass the 2 extra audit parameters
                    if (staffDAO.updateStaff(selectedUserId, name, user, email, role, "", currentAdminId, adminRoleStr)) {
                        JOptionPane.showMessageDialog(this, "Staff updated and logged!");
                    }
                }

                resetForm(); // Helper to clear fields and reset selectedUserId
                refreshTable();

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
        clearBtn = new JButton("Clear");
        btnPanel.add(clearBtn);
        btnPanel.add(saveBtn);

        gbc.gridx = 3; gbc.gridy = 2;
        form.add(btnPanel, gbc);

        return form;
    }

    private JPanel createTableArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Define Columns
        String[] columns = {"ID", "Full Name", "Username", "Email", "Role", "Status", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; } // Make table read-only
        };

        userTable = new JTable(tableModel);
        userTable.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                
                // If the hidden 'is_super' column (index 6) is true, show a badge
                boolean isSuper = (boolean) tableModel.getValueAt(row, 6);
                if (isSuper) {
                    setText(value.toString() + " ★"); // Add a star for Super Admins
                    setForeground(new Color(211, 84, 0)); // Orange color
                } else {
                    setForeground(Color.BLACK);
                }
                return c;
            }
        });
        
        userTable.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {

                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (value != null) {
                    String status = value.toString();
                    if (status.equalsIgnoreCase("Active")) {
                        setForeground(new java.awt.Color(46, 204, 113)); // Professional Green
                        setFont(getFont().deriveFont(java.awt.Font.BOLD));
                    } else {
                        setForeground(java.awt.Color.RED);
                        setFont(getFont().deriveFont(java.awt.Font.BOLD));
                    }
                }

                // Keep selection background consistent
                if (isSelected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(java.awt.Color.WHITE);
                }

                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                return c;
            }
        });
        
        // Inside createTableArea() after initializing userTable
        if (iAmSuperAdmin) {
            // 1. Create a Delete Button column renderer
            userTable.getColumnModel().getColumn(6).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    JButton deleteBtn = new JButton("Delete");
                    deleteBtn.setBackground(new Color(231, 76, 60)); // Red
                    deleteBtn.setForeground(Color.WHITE);
                    deleteBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
                    return deleteBtn;
                }
            });
        } else {
            // If not Super Admin, hide the Actions column or show "N/A"
            userTable.getColumnModel().getColumn(6).setMinWidth(0);
            userTable.getColumnModel().getColumn(6).setMaxWidth(0);
        }
        userTable.setRowHeight(30);
        userTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(userTable);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void addComponent(JPanel panel, Component comp, int x, int y, GridBagConstraints gbc) {
        gbc.gridx = x; gbc.gridy = y;
        panel.add(comp, gbc);
    }

    public void refreshTable() {
        try {
            // Clear the existing rows in the UI
            tableModel.setRowCount(0); 

            // Fetch the list of staff details from the DAO
            java.util.List<Object[]> staffList = staffDAO.getAllStaff();

            // Add each staff member as a new row in the table
            for (Object[] staff : staffList) {
                tableModel.addRow(staff);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading staff data: " + e.getMessage());
        }
    }
    
    private void resetForm() {
        nameField.setText("");
        userField.setText("");
        emailField.setText("");
        passField.setText("");
        roleCombo.setSelectedIndex(0);
        selectedUserId = -1; // Reset to Add Mode
        saveBtn.setText("Save User");
        saveBtn.setBackground(new Color(52, 152, 219)); // Reset to Blue
    }
}