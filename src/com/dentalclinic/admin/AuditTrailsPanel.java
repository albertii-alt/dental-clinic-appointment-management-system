package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.service.LogService;

public class AuditTrailsPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogService logService = new LogService();

    public AuditTrailsPanel() {
        // Set layout and the exact padding/margin used in previous panels (20px)
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // --- HEADER SECTION ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("System Activity Logs");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(new Color(44, 62, 80));
        
        JButton refreshButton = new JButton("Refresh Logs");
        refreshButton.setFocusable(false);
        refreshButton.setBackground(new Color(52, 152, 219));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> loadLogData());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(refreshButton, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- TABLE SECTION ---
        String[] columns = {"Log ID", "User Name", "Role", "Action", "Details", "Timestamp"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        logTable = new JTable(tableModel);
        logTable.setRowHeight(30);
        logTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        logTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Customizing column widths for better "Good UI" feel
        logTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        logTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        logTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Action
        logTable.getColumnModel().getColumn(4).setPreferredWidth(250); // Details

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        add(scrollPane, BorderLayout.CENTER);
        
        // --- DOUBLE CLICK LISTENER ---
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

        // Initial load
        loadLogData();
    }

    private void loadLogData() {
        try {
            tableModel.setRowCount(0); // Clear existing
            List<Object[]> logs = logService.getActivityLogs();
            for (Object[] row : logs) {
                tableModel.addRow(row);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading logs: " + e.getMessage());
        }
    }
    
    private void showLogDetailModal(int row) {
        String logId = tableModel.getValueAt(row, 0).toString();
        String userName = tableModel.getValueAt(row, 1).toString();
        String role = tableModel.getValueAt(row, 2).toString();
        String action = tableModel.getValueAt(row, 3).toString();
        String rawDetails = tableModel.getValueAt(row, 4).toString();
        String timestamp = tableModel.getValueAt(row, 5).toString();

        // --- EXTRACTION LOGIC ---
        String serviceDisplay = "N/A";
        String cleanDetails = rawDetails;

        // Check if the log contains our new separator
        if (rawDetails.contains("Service: ") && rawDetails.contains(" | ")) {
            int serviceStart = rawDetails.indexOf("Service: ") + 9;
            int separatorIdx = rawDetails.indexOf(" | ");

            serviceDisplay = rawDetails.substring(serviceStart, separatorIdx);
            cleanDetails = rawDetails.substring(separatorIdx + 3); // Take everything after the " | "
        }

        // --- UI CODE (As we designed before) ---
        JDialog detailDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Log Details", true);
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        contentPanel.setBackground(Color.WHITE);

        Font labelFont = new Font("SansSerif", Font.BOLD, 12);
        Font valueFont = new Font("SansSerif", Font.PLAIN, 13);

        // Header
        JLabel head = new JLabel("ACTIVITY LOG RECEIPT");
        head.setFont(new Font("SansSerif", Font.BOLD, 16));
        head.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(head);
        contentPanel.add(Box.createVerticalStrut(15));

        // Info Sections (Notice the Service Type is now its own centered section)
        contentPanel.add(createCenteredLabel("USER", userName + " (" + role + ")", labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(8));

        contentPanel.add(createCenteredLabel("SERVICE TYPE", serviceDisplay, labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(8));

        contentPanel.add(createCenteredLabel("ACTION", action, labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(8));

        contentPanel.add(createCenteredLabel("TIMESTAMP", timestamp, labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(20));

        // Description Box
        JLabel detLabel = new JLabel("FULL DESCRIPTION");
        detLabel.setFont(labelFont);
        detLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(detLabel);

        JTextArea detailsArea = new JTextArea(cleanDetails);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setEditable(false);
        detailsArea.setBackground(new Color(248, 249, 249));
        detailsArea.setMargin(new Insets(10,10,10,10));

        JScrollPane detailScroll = new JScrollPane(detailsArea);
        detailScroll.setPreferredSize(new Dimension(300, 80));
        detailScroll.setMaximumSize(new Dimension(300, 80));
        detailScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(detailScroll);

        contentPanel.add(Box.createVerticalStrut(20));
        JButton closeBtn = new JButton("Dismiss");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> detailDialog.dispose());
        contentPanel.add(closeBtn);

        detailDialog.add(contentPanel);
        detailDialog.pack();
        detailDialog.setLocationRelativeTo(this);
        detailDialog.setVisible(true);
    }

    // New Helper for Centered Stacked Labels
    private JPanel createCenteredLabel(String title, String value, Font lFont, Font vFont) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(lFont);
        t.setForeground(Color.GRAY);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel v = new JLabel(value);
        v.setFont(vFont);
        v.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(t);
        p.add(v);
        return p;
    }
}