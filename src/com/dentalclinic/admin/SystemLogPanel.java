package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.service.LogService;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class SystemLogPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogService logService = new LogService();
    
    // Session variables
    private int loggedUserId;
    private boolean isSuper;

    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color DANGER_RED = new Color(231, 76, 60);
    private final Color COLUMN_SHADE = new Color(242, 245, 249);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color BG_LIGHT = new Color(245, 247, 250);

    public SystemLogPanel(int userId, boolean isSuperAdmin) {
        this.loggedUserId = userId;
        this.isSuper = isSuperAdmin;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));
        setBackground(BG_LIGHT);

        // --- HEADER SECTION ---
        add(createHeader(), BorderLayout.NORTH);

        // --- TABLE SECTION ---
        add(createTableArea(), BorderLayout.CENTER);

        loadSystemLogs();
    }

    private JPanel createHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("System Logs");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_DARK);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);
        
        JButton exportButton = new JButton("Backup to CSV");
        styleButton(exportButton, new Color(52, 152, 219)); // A nice light blue
        exportButton.addActionListener(e -> exportToCSV());
        
        JButton refreshButton = new JButton("Refresh Logs");
        styleButton(refreshButton, PRIMARY_BLUE);
        refreshButton.addActionListener(e -> loadSystemLogs());
        
        JButton clearButton = new JButton("Clear All Logs");
        styleButton(clearButton, DANGER_RED);
        clearButton.addActionListener(e -> handleClearLogs());

        buttonPanel.add(refreshButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusable(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JScrollPane createTableArea() {
        String[] columns = {"ID", "Level", "Source Class", "Message", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        logTable = new JTable(tableModel);
        logTable.setRowHeight(45);
        logTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        logTable.setShowVerticalLines(false);
        logTable.setSelectionBackground(new Color(232, 241, 249));

        // Header Style
        JTableHeader header = logTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));

        // Column Renderers
        setupRenderers();

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        return scrollPane;
    }

    private void setupRenderers() {
        // Shaded ID Column
        logTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(COLUMN_SHADE);
                    c.setForeground(PRIMARY_BLUE);
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        // Log Level Color Renderer
        logTable.getColumnModel().getColumn(1).setCellRenderer(new LogLevelRenderer());
        
        // General Centering for Timestamp
        logTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        // Widths
        logTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(400);
    }

    private void handleClearLogs() {
        if (!isSuper) {
            JOptionPane.showMessageDialog(this, "Access Denied: Only a Super Administrator can clear system logs.", 
                                            "Insufficient Permissions", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPasswordField passwordField = new JPasswordField();
        Object[] message = {
            "CRITICAL: This will permanently delete all system history.",
            "A MANDATORY backup will be created first.",
            "Confirm Super Admin Password:",
            passwordField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Security Verification", 
                                                 JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String password = new String(passwordField.getPassword());

            // 1. Verify Identity
            if (logService.verifySuperAdminPassword(loggedUserId, password)) { 

                // 2. FORCE BACKUP BEFORE CLEARING
                JOptionPane.showMessageDialog(this, "A backup is required before clearing. Please choose a save location.");
                boolean backupSuccessful = exportToCSV(); // We will modify exportToCSV to return true/false

                if (backupSuccessful) {
                    // 3. ONLY CLEAR IF BACKUP WAS SAVED
                    String roleStr = isSuper ? "Super Admin" : "Admin"; 
                    if (logService.clearAllSystemLogs(loggedUserId, roleStr)) {
                        JOptionPane.showMessageDialog(this, "Backup created and logs cleared successfully.");
                        loadSystemLogs();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Clear cancelled: Backup was not saved.", 
                                                    "Action Aborted", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Invalid Password.", "Security Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadSystemLogs() {
        try {
            tableModel.setRowCount(0);
            List<Object[]> logs = logService.getSystemLogs(); 
            
            if (logs.isEmpty()) {   
                showNoDataScreen();
            } else {
                // Restore original panel layout if previously switched to "No Data" screen
                if (getLayout() instanceof GridBagLayout) {
                    removeAll();
                    setLayout(new BorderLayout(20, 20));
                    add(createHeader(), BorderLayout.NORTH);
                    add(createTableArea(), BorderLayout.CENTER);
                }
                for (Object[] row : logs) tableModel.addRow(row);
                revalidate();
                repaint();
            } 
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showNoDataScreen() {
        removeAll();
        setLayout(new GridBagLayout());
        JLabel noApp = new JLabel("No logs available yet.");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(new Color(189, 195, 199));
        add(noApp);
        revalidate();
        repaint();
    }

    private class LogLevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String level = (value != null) ? value.toString() : "";
            c.setHorizontalAlignment(SwingConstants.CENTER);
            c.setFont(c.getFont().deriveFont(Font.BOLD));

            if ("ERROR".equals(level)) c.setForeground(DANGER_RED);
            else if ("WARNING".equals(level)) c.setForeground(new Color(230, 126, 34));
            else c.setForeground(new Color(46, 204, 113));
            
            return c;
        }
    }
    
    // --- THE EXPORT LOGIC (Now returns boolean) ---
    private boolean exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save System Logs Backup");
        fileChooser.setSelectedFile(new File("SystemLogs_Backup_" + System.currentTimeMillis() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            // Ensure .csv extension
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Headers
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.print(tableModel.getColumnName(i) + (i == tableModel.getColumnCount() - 1 ? "" : ","));
                }
                writer.println();

                // Data
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object val = tableModel.getValueAt(row, col);
                        writer.print("\"" + (val == null ? "" : val.toString()) + "\"" 
                                     + (col == tableModel.getColumnCount() - 1 ? "" : ","));
                    }
                    writer.println();
                }

                // Record the export in Audit Trail
                logService.record(loggedUserId, isSuper ? "Super Admin" : "Admin", "Export Logs", "Manual backup created: " + file.getName());
                return true;

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false; // User clicked cancel
    }
    public void cleanup() {
    System.out.println("Cleaning up SystemLogPanel...");
}
}