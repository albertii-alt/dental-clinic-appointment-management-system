package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.LogController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dentalclinic.model.Appointment;

public class CancelledAppointmentsPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final String CACHE_KEY = "STAFF_CANCELLED_APPOINTMENTS";
    private static final Map<String, CacheEntry> CANCELLED_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private final LogController logController = new LogController();
    private int currentStaffId;
    private String currentStaffName;
    private SwingWorker<List<Object[]>, Void> loadWorker;
    private long loadRequestId = 0;
    private JLabel emptyLabel;

    private static class CacheEntry {
        private final List<Object[]> rows;
        private final long createdAtMs;

        private CacheEntry(List<Object[]> rows, long createdAtMs) {
            this.rows = rows;
            this.createdAtMs = createdAtMs;
        }
    }

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color DANGER = new Color(231, 76, 60);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public CancelledAppointmentsPanel(int staffId, String staffName) {
        this.currentStaffId = staffId;
        this.currentStaffName = staffName;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- THE MAIN CARD CONTAINER ---
        JPanel cardContainer = new JPanel(new BorderLayout(0, 20));
        cardContainer.setBackground(CARD);
        cardContainer.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // --- HEADER SECTION ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        
        // Inside the Header Section of CancelledAppointmentsPanel
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(CARD);

        JButton clearButton = new JButton("Clear History");
        clearButton.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TRASH_ALT, 13, Color.WHITE));
        clearButton.setBackground(DANGER);
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearButton.setFocusable(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> handleClearHistory());

        rightPanel.add(clearButton);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { CANCELLED_CACHE.remove(CACHE_KEY); loadCancelledData(true); });
        rightPanel.add(refreshBtn);

        header.add(rightPanel, BorderLayout.EAST);
        
        JLabel title = new JLabel("Cancellation History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("View records of cancelled or declined appointment requests.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setBackground(CARD);
        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        cardContainer.add(header, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"ID", "Patient Name", "Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        emptyLabel = new JLabel("No cancelled records found.", SwingConstants.CENTER);
        emptyLabel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        emptyLabel.setForeground(Color.LIGHT_GRAY);
        emptyLabel.setVisible(false);
        cardContainer.add(emptyLabel, BorderLayout.SOUTH);

        // Double click just to see details (Read-Only)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showReadOnlyDetails();
                }
            }
        });

        add(cardContainer, BorderLayout.CENTER);
        loadCancelledData(false);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(header.getBackground());
                setForeground(header.getForeground());
                setFont(header.getFont());
                setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
                return this;
            }
        });
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));
        header.setReorderingAllowed(false);
        
        // Status Column Color (Render Cancelled as Red)
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(DANGER);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    public void loadCancelledData(boolean forceRefresh) {
        CacheEntry cached = CANCELLED_CACHE.get(CACHE_KEY);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            renderRows(cached.rows);
            return;
        }

        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        final long requestId = ++loadRequestId;
        table.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        loadWorker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return appointmentController.getCancelledRequestsWithNames();
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Object[]> rows = get();
                    CANCELLED_CACHE.put(CACHE_KEY, new CacheEntry(new ArrayList<>(rows), System.currentTimeMillis()));
                    renderRows(rows);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(CancelledAppointmentsPanel.this,
                            "Failed to load cancelled appointments: " + e.getMessage(),
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    table.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        loadWorker.execute();
    }

    private void renderRows(List<Object[]> rows) {
        model.setRowCount(0);
        if (rows == null || rows.isEmpty()) {
            table.setVisible(false);
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);
        table.setVisible(true);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private void showReadOnlyDetails() {
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, 
            "This is a cancelled record for historical purposes.\nTo book this patient again, they must create a new request.", 
            "Record Information", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleClearHistory() {
        // 1. Double Confirmation
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to permanently delete all cancellation history?\n" +
            "A mandatory CSV backup will be created first.", 
            "Confirm Permanent Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // 2. Force Backup
            if (exportToCSV()) {
                // 3. Delete from Database
                if (appointmentController.clearAllCancelledAppointments()) {
                    // 4. Record the action in Audit Trail (Important!)
                    logController.record(
                        currentStaffId, "Staff", "Clear History", "All cancelled/declined appointments were archived and deleted."
                    );

                    JOptionPane.showMessageDialog(this, "History cleared successfully.");
                    CANCELLED_CACHE.remove(CACHE_KEY);
                    loadCancelledData(true);
                }else {
    JOptionPane.showMessageDialog(this, "Error: Could not clear database records.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private boolean exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("Cancelled_Apps_Backup_" + System.currentTimeMillis() + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(fileChooser.getSelectedFile()))) {
                // Write Headers
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.print(model.getColumnName(i) + (i == model.getColumnCount() - 1 ? "" : ","));
                }
                writer.println();

                // Write Data
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        Object val = model.getValueAt(i, j);
                        writer.print("\"" + (val == null ? "" : val.toString()) + "\"" + (j == model.getColumnCount() - 1 ? "" : ","));
                    }
                    writer.println();
                }
                return true;
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this, "Backup failed: " + e.getMessage());
                return false;
            }
        }
        return false; // User cancelled backup
    }
}
