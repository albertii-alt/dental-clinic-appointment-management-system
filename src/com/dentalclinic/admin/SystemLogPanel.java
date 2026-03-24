package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;
import com.dentalclinic.service.LogService;

public class SystemLogPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogService logService = new LogService();
    
    // NEW: Session variables
    private int loggedUserId;
    private boolean isSuper;

    public SystemLogPanel(int userId, boolean isSuperAdmin) {
        this.loggedUserId = userId;
        this.isSuper = isSuperAdmin;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("System Maintenance Logs");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(44, 62, 80));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton refreshButton = new JButton("Refresh Logs");
        refreshButton.setBackground(new Color(52, 152, 219));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> loadSystemLogs());

        JButton clearButton = new JButton("Clear All Logs");
        clearButton.setBackground(new Color(231, 76, 60));
        clearButton.setForeground(Color.WHITE);
        clearButton.addActionListener(e -> handleClearLogs());

        buttonPanel.add(refreshButton);
        buttonPanel.add(clearButton);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- TABLE SECTION (Keeping your existing table logic) ---
        String[] columns = {"ID", "Level", "Source Class", "Message", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        logTable = new JTable(tableModel);
        logTable.setRowHeight(35);
        logTable.getColumnModel().getColumn(1).setCellRenderer(new LogLevelRenderer());
        add(new JScrollPane(logTable), BorderLayout.CENTER);

        loadSystemLogs();
    }

    private void handleClearLogs() {
        // 1. Check if the user is even a Super Admin first
        if (!isSuper) {
            JOptionPane.showMessageDialog(this, 
                "Access Denied: Only a Super Administrator can clear system logs.", 
                "Insufficient Permissions", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. If they are Super Admin, ask for password
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {
            "CRITICAL: This will permanently delete all system history.",
            "Confirm Super Admin Password:",
            passwordField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Security Verification", 
                     JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword());
            
            // 3. Verify password against database for this specific Super Admin
            if (logService.verifySuperAdminPassword(loggedUserId, password)) { 
                if (logService.clearAllSystemLogs()) {
                    JOptionPane.showMessageDialog(this, "System logs cleared successfully.");
                    loadSystemLogs();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Password.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadSystemLogs() {
        try {
            tableModel.setRowCount(0);
            List<Object[]> logs = logService.getSystemLogs(); 
            for (Object[] row : logs) tableModel.addRow(row);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // Keep your LogLevelRenderer inner class here...
    private class LogLevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String level = (value != null) ? value.toString() : "";
            setHorizontalAlignment(SwingConstants.CENTER);
            if ("ERROR".equals(level)) setForeground(new Color(231, 76, 60));
            else if ("WARNING".equals(level)) setForeground(new Color(230, 126, 34));
            else setForeground(new Color(46, 204, 113));
            return this;
        }
    }
}