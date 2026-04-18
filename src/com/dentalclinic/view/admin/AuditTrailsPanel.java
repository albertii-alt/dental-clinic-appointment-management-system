package com.dentalclinic.view.admin;

import com.dentalclinic.controller.LogController;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class AuditTrailsPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private final LogController logController = new LogController();
    private int currentAdminId;
    private boolean isSuperAdmin;

    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color COLUMN_SHADE = new Color(242, 245, 249); // Subtle background shade
    private final Color NAME_TEXT_COLOR = new Color(41, 128, 185);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color BG_LIGHT = new Color(245, 247, 250);

    public AuditTrailsPanel(int adminId, boolean isSuper) {
        this.currentAdminId = adminId;
        this.isSuperAdmin = isSuper;

        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));
        setBackground(BG_LIGHT);

        // --- HEADER SECTION ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Audit Trails");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_DARK);
        
        JButton refreshButton = new JButton("Refresh Logs");
        styleHeaderButton(refreshButton);
        refreshButton.addActionListener(e -> loadLogData());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(refreshButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
        
        // Inside the Header Section of your constructor
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        // ONLY add the archive button if they are a Super Admin
        if (isSuperAdmin) {
            JButton archiveButton = new JButton("Archive & Clear");
            styleHeaderButton(archiveButton);
            archiveButton.setBackground(new Color(230, 126, 34)); // Orange color for "Warning" action
            archiveButton.addActionListener(e -> handleArchiveLogs());
            buttonPanel.add(archiveButton);
        }

        buttonPanel.add(refreshButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        // --- TABLE SECTION ---
        String[] columns = {"Log ID", "User Name", "Role", "Action", "Details", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        logTable = new JTable(tableModel);
        styleTable(logTable);
        
        logTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = logTable.getSelectedRow();
                    if (row != -1) {
                        showLogDetailModal(row);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        loadLogData();
    }

    private void styleHeaderButton(JButton btn) {
        btn.setFocusable(false);
        btn.setBackground(PRIMARY_BLUE);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));

        // --- 1. SPECIAL HEADER RENDERER (For the "User Name" Header Background) ---
        table.getColumnModel().getColumn(1).setHeaderRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setBackground(COLUMN_SHADE); // Shaded background like your image
                l.setForeground(TEXT_DARK);
                l.setFont(new Font("Segoe UI", Font.BOLD, 14));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 1, new Color(220, 220, 220)));
                return l;
            }
        });

        // --- 2. SPECIAL CELL RENDERER (For the User Name Data Cells) ---
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setBackground(COLUMN_SHADE); // Keeps the column shaded down the rows
                    c.setForeground(NAME_TEXT_COLOR); // Blue text as requested
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        // Column Widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60); 
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
    }

    private void loadLogData() {
        try {
            tableModel.setRowCount(0); 
            List<Object[]> logs = logController.getActivityLogs(); 
            
            if (logs.isEmpty()) {   
                showNoDataScreen();
            } else {
                for (Object[] row : logs) {
                    int performerId = (int) row[1]; 
                    if (performerId == currentAdminId) {
                        row[2] = "You"; 
                    }

                    Object[] displayRow = new Object[] {
                        row[0], row[2], row[3], row[4], row[5], row[6]
                    };
                    tableModel.addRow(displayRow);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading logs: " + e.getMessage());
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
    
    private void showLogDetailModal(int row) {
        String logId = tableModel.getValueAt(row, 0).toString();
        String userName = tableModel.getValueAt(row, 1).toString();
        String role = tableModel.getValueAt(row, 2).toString();
        String action = tableModel.getValueAt(row, 3).toString();
        String rawDetails = tableModel.getValueAt(row, 4).toString();
        String timestamp = tableModel.getValueAt(row, 5).toString();

        String serviceDisplay = "N/A";
        String cleanDetails = rawDetails;

        if (rawDetails.contains("Service: ") && rawDetails.contains(" | ")) {
            int serviceStart = rawDetails.indexOf("Service: ") + 9;
            int separatorIdx = rawDetails.indexOf(" | ");
            serviceDisplay = rawDetails.substring(serviceStart, separatorIdx);
            cleanDetails = rawDetails.substring(separatorIdx + 3);
        }

        JDialog detailDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Activity Details", true);
        detailDialog.setLayout(new BorderLayout());

        // Main panel with BoxLayout (vertical) to ensure all content is visible
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        mainPanel.setBackground(Color.WHITE);

        // Header section
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel head = new JLabel("LOG ENTRY #" + logId);
        head.setFont(new Font("Segoe UI", Font.BOLD, 20));
        head.setForeground(PRIMARY_BLUE);
        headerPanel.add(head, BorderLayout.WEST);
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // Info panel - using GridBagLayout for proper alignment
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font labelFont = new Font("Segoe UI", Font.BOLD, 12);
        Font valueFont = new Font("Segoe UI", Font.PLAIN, 14);
        Color labelColor = new Color(127, 140, 141);

        // Row 0: Performed By
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel performedLabel = new JLabel("PERFORMED BY:");
        performedLabel.setFont(labelFont);
        performedLabel.setForeground(labelColor);
        infoPanel.add(performedLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel performedValue = new JLabel(userName + " (" + role + ")");
        performedValue.setFont(valueFont);
        performedValue.setForeground(TEXT_DARK);
        infoPanel.add(performedValue, gbc);

        // Row 1: Service Category
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel serviceLabel = new JLabel("SERVICE CATEGORY:");
        serviceLabel.setFont(labelFont);
        serviceLabel.setForeground(labelColor);
        infoPanel.add(serviceLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel serviceValue = new JLabel(serviceDisplay);
        serviceValue.setFont(valueFont);
        serviceValue.setForeground(TEXT_DARK);
        infoPanel.add(serviceValue, gbc);

        // Row 2: Action Taken
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        JLabel actionLabel = new JLabel("ACTION TAKEN:");
        actionLabel.setFont(labelFont);
        actionLabel.setForeground(labelColor);
        infoPanel.add(actionLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel actionValue = new JLabel(action.toUpperCase());
        actionValue.setFont(valueFont);
        actionValue.setForeground(PRIMARY_BLUE);
        infoPanel.add(actionValue, gbc);

        // Row 3: Timestamp
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        JLabel timeLabel = new JLabel("TIMESTAMP:");
        timeLabel.setFont(labelFont);
        timeLabel.setForeground(labelColor);
        infoPanel.add(timeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        JLabel timeValue = new JLabel(timestamp);
        timeValue.setFont(valueFont);
        timeValue.setForeground(TEXT_DARK);
        infoPanel.add(timeValue, gbc);

        mainPanel.add(infoPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Details section - left-aligned for easy reading
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBackground(Color.WHITE);

        JLabel detLabel = new JLabel("DETAILED LOG DESCRIPTION");
        detLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        detLabel.setForeground(labelColor);
        detailsPanel.add(detLabel, BorderLayout.NORTH);

        JTextArea detailsArea = new JTextArea(cleanDetails);
        detailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setEditable(false);
        detailsArea.setBackground(new Color(248, 249, 250));
        detailsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            new EmptyBorder(12, 12, 12, 12)
        ));

        // Set reasonable size for text area
        detailsArea.setRows(5);
        detailsArea.setColumns(50);

        JScrollPane detailScroll = new JScrollPane(detailsArea);
        detailScroll.setBorder(null);
        detailScroll.getViewport().setBackground(new Color(248, 249, 250));
        detailScroll.setPreferredSize(new Dimension(500, 120));
        detailsPanel.add(detailScroll, BorderLayout.CENTER);

        mainPanel.add(detailsPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);

        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(PRIMARY_BLUE);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(100, 35));
        closeBtn.addActionListener(e -> detailDialog.dispose());

        buttonPanel.add(closeBtn);
        mainPanel.add(buttonPanel);

        // Wrap mainPanel in a JScrollPane to handle very long content
        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setBorder(null);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        detailDialog.add(mainScrollPane);
        detailDialog.setSize(600, 550);
        detailDialog.setMinimumSize(new Dimension(550, 450));
        detailDialog.setLocationRelativeTo(this);
        detailDialog.setVisible(true);
    }

    private JPanel createCenteredLabel(String title, String value, Font lFont, Font vFont) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(lFont);
        t.setForeground(new Color(149, 165, 166));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel v = new JLabel(value);
        v.setFont(vFont);
        v.setForeground(TEXT_DARK);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(t);
        p.add(Box.createVerticalStrut(2));
        p.add(v);
        return p;
    }
    
    private void handleArchiveLogs() {
    if (!isSuperAdmin) {
        JOptionPane.showMessageDialog(this, "Access Denied: Only Super Admins can archive the Audit Trail.");
        return;
    }

    // Security Verification
    JPasswordField passwordField = new JPasswordField();
    Object[] message = {
        "ARCHIVE WARNING: This will move all activity history to a CSV file and clear this table.",
        "Enter Super Admin Password:", passwordField
    };

    int option = JOptionPane.showConfirmDialog(this, message, "Archive Activity Logs", JOptionPane.OK_CANCEL_OPTION);

    if (option == JOptionPane.OK_OPTION) {
        if (logController.verifySuperAdminPassword(currentAdminId, new String(passwordField.getPassword()))) {
            
            // 1. Mandatory Backup
            if (exportToCSV()) {
                // 2. Clear Table
                if (logController.archiveActivityLogs(currentAdminId, "Super Admin")) {
                    // 3. Record the Archiving as the NEW first entry
                    logController.record(currentAdminId, "Super Admin", "Archive Action", "Audit Trail was cleared and archived to CSV.");
                    
                    JOptionPane.showMessageDialog(this, "Audit Trail archived and cleared successfully.");
                    loadLogData();
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid Password.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

    private boolean exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("AuditTrail_Archive_" + System.currentTimeMillis() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()))) {
                // Write Headers
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.print(tableModel.getColumnName(i) + (i == tableModel.getColumnCount() - 1 ? "" : ","));
                }
                writer.println();

                // Write Data
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Object val = tableModel.getValueAt(i, j);
                        writer.print("\"" + (val == null ? "" : val.toString()) + "\"" + (j == tableModel.getColumnCount() - 1 ? "" : ","));
                    }
                    writer.println();
                }
                return true;
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this, "Backup failed: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public void cleanup() {
    System.out.println("Cleaning up AuditTrailsPanel...");
}
}
