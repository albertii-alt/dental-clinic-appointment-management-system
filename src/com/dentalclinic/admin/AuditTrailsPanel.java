package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.service.LogService;

public class AuditTrailsPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;
    private LogService logService = new LogService();
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
            List<Object[]> logs = logService.getActivityLogs(); 
            
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
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(25, 40, 25, 40));
        contentPanel.setBackground(Color.WHITE);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 12);
        Font valueFont = new Font("Segoe UI", Font.PLAIN, 14);

        JLabel head = new JLabel("LOG ENTRY #" + logId);
        head.setFont(new Font("Segoe UI", Font.BOLD, 18));
        head.setForeground(PRIMARY_BLUE);
        head.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(head);
        contentPanel.add(Box.createVerticalStrut(20));

        contentPanel.add(createCenteredLabel("PERFORMED BY", userName + " (" + role + ")", labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createCenteredLabel("SERVICE CATEGORY", serviceDisplay, labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createCenteredLabel("ACTION TAKEN", action.toUpperCase(), labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(createCenteredLabel("TIMESTAMP", timestamp, labelFont, valueFont));
        contentPanel.add(Box.createVerticalStrut(25));

        JLabel detLabel = new JLabel("DETAILED LOG DESCRIPTION");
        detLabel.setFont(labelFont);
        detLabel.setForeground(new Color(127, 140, 141));
        detLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(detLabel);
        contentPanel.add(Box.createVerticalStrut(8));

        JTextArea detailsArea = new JTextArea(cleanDetails);
        detailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setEditable(false);
        detailsArea.setBackground(new Color(248, 249, 250));
        detailsArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane detailScroll = new JScrollPane(detailsArea);
        detailScroll.setPreferredSize(new Dimension(350, 100));
        detailScroll.setBorder(new LineBorder(new Color(230, 230, 230)));
        detailScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(detailScroll);

        contentPanel.add(Box.createVerticalStrut(25));
        JButton closeBtn = new JButton("Dismiss");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> detailDialog.dispose());
        contentPanel.add(closeBtn);

        detailDialog.add(contentPanel);
        detailDialog.pack();
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
}