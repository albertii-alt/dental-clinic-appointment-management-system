package com.dentalclinic.view.admin;

import com.dentalclinic.controller.LogController;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class SystemLogPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final String CACHE_KEY = "SYSTEM_LOGS";
    private static final Map<String, CacheEntry> SYSTEM_CACHE = new ConcurrentHashMap<>();

    private JTable logTable;
    private DefaultTableModel tableModel;
    private final LogController logController = new LogController();
    private SwingWorker<List<Object[]>, Void> loadWorker;
    private long loadRequestId = 0;
    
    // Session variables
    private int loggedUserId;
    private boolean isSuper;

    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color DANGER_RED = new Color(231, 76, 60);
    private final Color COLUMN_SHADE = new Color(242, 245, 249);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color BG_LIGHT = new Color(245, 247, 250);

    // Date formatter for timestamps
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static class CacheEntry {
        private final List<Object[]> data;
        private final long createdAtMs;

        private CacheEntry(List<Object[]> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

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

        loadSystemLogs(false);
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
        styleButton(exportButton, new Color(52, 152, 219));
        exportButton.addActionListener(e -> exportToCSV());
        
        JButton refreshButton = new JButton("Refresh Logs");
        styleButton(refreshButton, PRIMARY_BLUE);
        refreshButton.addActionListener(e -> loadSystemLogs(true));
        
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

        // Enable tooltips
        logTable.setToolTipText("Double-click any row to view full message");

        // Header Style
        JTableHeader header = logTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));

        // Column Renderers
        setupRenderers();
        
        // Add mouse listener for double-click
        addTableClickListener();

        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        return scrollPane;
    }
    
    /**
     * Helper method to safely get string value from any object type
     */
    private String getStringValue(Object obj) {
        if (obj == null) return "";
        if (obj instanceof java.util.Date) {
            return dateFormat.format((java.util.Date) obj);
        }
        return obj.toString();
    }
    
    /**
     * Add mouse click listener to show full message when any row is double-clicked
     */
    private void addTableClickListener() {
        logTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only trigger on double-click
                if (e.getClickCount() == 2) {
                    int row = logTable.getSelectedRow();
                    
                    if (row != -1) {
                        try {
                            String logId = getStringValue(tableModel.getValueAt(row, 0));
                            String logLevel = getStringValue(tableModel.getValueAt(row, 1));
                            String sourceClass = getStringValue(tableModel.getValueAt(row, 2));
                            String fullMessage = getStringValue(tableModel.getValueAt(row, 3));
                            String timestamp = getStringValue(tableModel.getValueAt(row, 4));
                            
                            showFullMessageDialog(fullMessage, logLevel, sourceClass, timestamp, logId);
                        } catch (Exception ex) {
                            System.err.println("Error showing dialog: " + ex.getMessage());
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(SystemLogPanel.this, 
                                "Error displaying log details: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });
    }
    
    /**
    * Show dialog with full message details (compact version)
    */
   private void showFullMessageDialog(String message, String logLevel, String sourceClass, String timestamp, String logId) {
       // Calculate optimal dialog size based on message length
       int messageLines = message.length() / 80 + 3; // Approximate lines needed
       int dialogHeight = Math.min(500, Math.max(350, 250 + (messageLines * 15)));
       int dialogWidth = Math.min(700, Math.max(500, Math.min(700, message.length() / 2 + 300)));

       JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Log Details - ID: " + logId, true);
       dialog.setLayout(new BorderLayout(10, 10));

       // Main panel with padding
       JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
       mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
       mainPanel.setBackground(Color.WHITE);

       // Info panel - using GridBagLayout for compact display
       JPanel infoPanel = new JPanel(new GridBagLayout());
       infoPanel.setBackground(Color.WHITE);
       infoPanel.setBorder(BorderFactory.createCompoundBorder(
           BorderFactory.createLineBorder(new Color(220, 220, 220)),
           new EmptyBorder(10, 10, 10, 10)
       ));

       GridBagConstraints gbc = new GridBagConstraints();
       gbc.insets = new Insets(4, 8, 4, 8);
       gbc.fill = GridBagConstraints.HORIZONTAL;

       // Row 0: Log ID
       gbc.gridx = 0;
       gbc.gridy = 0;
       gbc.weightx = 0;
       JLabel idLabel = new JLabel("Log ID:");
       idLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
       infoPanel.add(idLabel, gbc);

       gbc.gridx = 1;
       gbc.weightx = 1;
       JLabel idValue = new JLabel(logId);
       idValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
       infoPanel.add(idValue, gbc);

       // Row 1: Level
       gbc.gridx = 0;
       gbc.gridy = 1;
       gbc.weightx = 0;
       JLabel levelLabel = new JLabel("Level:");
       levelLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
       infoPanel.add(levelLabel, gbc);

       gbc.gridx = 1;
       gbc.weightx = 1;
       JLabel levelValue = new JLabel(logLevel);
       levelValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
       // Color code the level
       if ("ERROR".equals(logLevel)) {
           levelValue.setForeground(DANGER_RED);
       } else if ("WARNING".equals(logLevel)) {
           levelValue.setForeground(new Color(230, 126, 34));
       } else {
           levelValue.setForeground(new Color(46, 204, 113));
       }
       infoPanel.add(levelValue, gbc);

       // Row 2: Source
       gbc.gridx = 0;
       gbc.gridy = 2;
       gbc.weightx = 0;
       JLabel sourceLabel = new JLabel("Source:");
       sourceLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
       infoPanel.add(sourceLabel, gbc);

       gbc.gridx = 1;
       gbc.weightx = 1;
       JLabel sourceValue = new JLabel(sourceClass);
       sourceValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
       infoPanel.add(sourceValue, gbc);

       // Row 3: Timestamp
       gbc.gridx = 0;
       gbc.gridy = 3;
       gbc.weightx = 0;
       JLabel timeLabel = new JLabel("Timestamp:");
       timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
       infoPanel.add(timeLabel, gbc);

       gbc.gridx = 1;
       gbc.weightx = 1;
       JLabel timeValue = new JLabel(timestamp);
       timeValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
       infoPanel.add(timeValue, gbc);

       mainPanel.add(infoPanel, BorderLayout.NORTH);

       // Message area - with better sizing
       JTextArea messageArea = new JTextArea(message);
       messageArea.setEditable(false);
       messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
       messageArea.setLineWrap(true);
       messageArea.setWrapStyleWord(true);
       messageArea.setBackground(new Color(250, 251, 252));
       messageArea.setBorder(BorderFactory.createCompoundBorder(
           BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)), "Full Message"),
           new EmptyBorder(10, 10, 10, 10)
       ));

       // Calculate optimal rows for text area (max 15, min 5)
       int rows = Math.min(15, Math.max(5, message.length() / 80 + 2));
       messageArea.setRows(rows);

       JScrollPane scrollPane = new JScrollPane(messageArea);
       scrollPane.setBorder(null);
       scrollPane.getViewport().setBackground(new Color(250, 251, 252));

       mainPanel.add(scrollPane, BorderLayout.CENTER);

       // Button panel - compact
       JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
       buttonPanel.setBackground(Color.WHITE);
       buttonPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

       JButton copyButton = new JButton("Copy to Clipboard");
       styleButton(copyButton, new Color(52, 152, 219));
       copyButton.setPreferredSize(new Dimension(140, 35));
       copyButton.addActionListener(e -> {
           StringSelection stringSelection = new StringSelection(message);
           Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
           JOptionPane.showMessageDialog(dialog, "Message copied to clipboard!");
       });

       JButton closeButton = new JButton("Close");
       styleButton(closeButton, PRIMARY_BLUE);
       closeButton.setPreferredSize(new Dimension(100, 35));
       closeButton.addActionListener(e -> dialog.dispose());

       buttonPanel.add(copyButton);
       buttonPanel.add(closeButton);

       mainPanel.add(buttonPanel, BorderLayout.SOUTH);

       dialog.add(mainPanel);
       dialog.setSize(dialogWidth, dialogHeight);
       dialog.setMinimumSize(new Dimension(450, 300));
       dialog.setLocationRelativeTo(this);
       dialog.setVisible(true);
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
        
        // Message column with tooltip (shows full message on hover)
        logTable.getColumnModel().getColumn(3).setCellRenderer(new MessageTooltipRenderer());
        
        // Timestamp column renderer (handles Timestamp objects)
        logTable.getColumnModel().getColumn(4).setCellRenderer(new TimestampRenderer());
        
        // Widths
        logTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        logTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        logTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        logTable.getColumnModel().getColumn(3).setPreferredWidth(350);
        logTable.getColumnModel().getColumn(4).setPreferredWidth(150);
    }
    
    /**
     * Custom renderer for Timestamp column
     */
    private class TimestampRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String formattedDate = "";
            if (value instanceof java.util.Date) {
                formattedDate = dateFormat.format((java.util.Date) value);
            } else if (value != null) {
                formattedDate = value.toString();
            }
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, formattedDate, isSelected, hasFocus, row, column);
            c.setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
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
            if (logController.verifySuperAdminPassword(loggedUserId, password)) { 

                // 2. FORCE BACKUP BEFORE CLEARING
                JOptionPane.showMessageDialog(this, "A backup is required before clearing. Please choose a save location.");
                boolean backupSuccessful = exportToCSV();

                if (backupSuccessful) {
                    // 3. ONLY CLEAR IF BACKUP WAS SAVED
                    String roleStr = isSuper ? "Super Admin" : "Admin"; 
                    if (logController.clearAllSystemLogs(loggedUserId, roleStr)) {
                        JOptionPane.showMessageDialog(this, "Backup created and logs cleared successfully.");
                        loadSystemLogs(true);
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

    private void loadSystemLogs(boolean forceRefresh) {
        CacheEntry cached = SYSTEM_CACHE.get(CACHE_KEY);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            renderRows(cached.data);
            return;
        }

        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        final long requestId = ++loadRequestId;
        logTable.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        loadWorker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> rawLogs = logController.getSystemLogs();
                List<Object[]> rows = new ArrayList<>();
                for (Object[] row : rawLogs) {
                    rows.add(new Object[]{row[0], row[1], row[2], row[3], row[4]});
                }
                return rows;
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Object[]> rows = get();
                    SYSTEM_CACHE.put(CACHE_KEY, new CacheEntry(new ArrayList<>(rows), System.currentTimeMillis()));
                    renderRows(rows);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(SystemLogPanel.this,
                            "Error: " + e.getMessage(),
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    logTable.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        loadWorker.execute();
    }

    private void renderRows(List<Object[]> rows) {
        tableModel.setRowCount(0);
        for (Object[] row : rows) {
            tableModel.addRow(row);
        }
        revalidate();
        repaint();
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

    /**
     * Custom renderer for Log Level column with colors
     */
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
    
    /**
     * Custom renderer for Message column with tooltip (shows full message on hover)
     */
    private class MessageTooltipRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String message = (value != null) ? value.toString() : "";
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, message, isSelected, hasFocus, row, column);
            
            // Truncate long messages for display
            if (message.length() > 100) {
                c.setText(message.substring(0, 97) + "...");
                c.setToolTipText("<html><div style='width: 400px; padding: 10px;'>" + 
                                 escapeHtml(message) + "</div></html>");
            } else {
                c.setText(message);
                c.setToolTipText("<html><div style='width: 400px; padding: 10px;'>" + 
                                 escapeHtml(message) + "</div></html>");
            }
            
            c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            return c;
        }
        
        private String escapeHtml(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\"", "&quot;")
                      .replace("'", "&#39;")
                      .replace("\n", "<br>");
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
                        String strVal = "";
                        if (val instanceof java.util.Date) {
                            strVal = dateFormat.format((java.util.Date) val);
                        } else if (val != null) {
                            strVal = val.toString();
                        }
                        writer.print("\"" + strVal.replace("\"", "\"\"") + "\"" 
                                     + (col == tableModel.getColumnCount() - 1 ? "" : ","));
                    }
                    writer.println();
                }

                // Record the export in Audit Trail
                logController.record(loggedUserId, isSuper ? "Super Admin" : "Admin", "Export Logs", "Manual backup created: " + file.getName());
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
