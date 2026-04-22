package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.model.Appointment;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatientHistoryPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final String CACHE_KEY = "STAFF_TREATMENT_HISTORY";
    private static final Map<String, CacheEntry> HISTORY_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private final AppointmentController appointmentController = new AppointmentController();
    private SwingWorker<List<Object[]>, Void> loadWorker;
    private long loadRequestId = 0;
    
    private boolean isDentist;

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    private static class CacheEntry {
        private final List<Object[]> rows;
        private final long createdAtMs;

        private CacheEntry(List<Object[]> rows, long createdAtMs) {
            this.rows = rows;
            this.createdAtMs = createdAtMs;
        }
    }

    public PatientHistoryPanel(boolean isDentist) {
        this.isDentist = isDentist;
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- MAIN CARD ---
        JPanel cardContainer = new JPanel(new BorderLayout(0, 20));
        cardContainer.setBackground(CARD);
        cardContainer.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // --- HEADER & SEARCH AREA ---
        JPanel headerArea = new JPanel(new BorderLayout());
        headerArea.setBackground(CARD);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(CARD);
        JLabel title = new JLabel("Patient Treatment History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(PRIMARY);
        JLabel subtitle = new JLabel("Review past procedures and clinical notes.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        // Modern Search Bar
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchBox.setBackground(CARD);
        JLabel searchIcon = new JLabel("Search Name: ");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(250, 35));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        
        searchBox.add(searchIcon);
        searchBox.add(searchField);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setBackground(CARD);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { HISTORY_CACHE.remove(CACHE_KEY); loadHistoryData(true); });
        headerRight.add(searchBox);
        headerRight.add(refreshBtn);

        headerArea.add(titlePanel, BorderLayout.WEST);
        headerArea.add(headerRight, BorderLayout.EAST);
        headerArea.add(searchBox, BorderLayout.EAST);
        cardContainer.add(headerArea, BorderLayout.NORTH);

        // --- TABLE AREA ---
        String[] columns = {"ID", "Patient Name", "Service Performed", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        // Hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        // --- LISTENERS ---
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { showFullHistoryDetail(); }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, 1));
            }
        });

        add(cardContainer, BorderLayout.CENTER);
        loadHistoryData(false);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 40));

        // Color-code Status column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                String status = value != null ? value.toString() : "";
                switch (status) {
                    case "Completed": setForeground(SUCCESS); break;
                    case "Expired":   setForeground(new Color(192, 57, 43)); break;
                    case "No Show":   setForeground(new Color(127, 140, 141)); break;
                    default:          setForeground(TEXT); break;
                }
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    public void loadHistoryData(boolean forceRefresh) {
        CacheEntry cached = HISTORY_CACHE.get(CACHE_KEY);
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
                return appointmentController.getTreatmentHistory();
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Object[]> rows = get();
                    HISTORY_CACHE.put(CACHE_KEY, new CacheEntry(new ArrayList<>(rows), System.currentTimeMillis()));
                    renderRows(rows);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PatientHistoryPanel.this,
                            "Failed to load treatment history: " + e.getMessage(),
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
            return;
        }
        for (Object[] row : rows) {
            // row: [0]=id, [1]=name, [2]=service, [3]=date, [4]=time, [5]=notes, [6]=status
            model.addRow(new Object[]{ row[0], row[1], row[2], row[3], row[4], row[6] });
        }
    }
    
    private void showFullHistoryDetail() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int modelRow = table.convertRowIndexToModel(row);
        int appId = (int) model.getValueAt(modelRow, 0);
        String patientName = (String) model.getValueAt(modelRow, 1);

        try {
            List<Appointment> allApps = appointmentController.getAllAppointments();
            Appointment app = allApps.stream()
                                     .filter(a -> a.getAppointmentId() == appId)
                                     .findFirst().orElse(null);

            if (app != null) {
                JPanel panel = new JPanel(new BorderLayout(15, 15));
                panel.setPreferredSize(new Dimension(450, 400));
                panel.setBackground(Color.WHITE);

                // --- HEADER INFO ---
                JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                infoPanel.setBackground(Color.WHITE);
                infoPanel.add(new JLabel("<html><b style='color:#2980b9; font-size:16px;'>TREATMENT RECORD #" + appId + "</b></html>"));
                infoPanel.add(new JSeparator());
                infoPanel.add(new JLabel("Patient: " + patientName));
                infoPanel.add(new JLabel("Service: " + app.getServiceType()));
                infoPanel.add(new JLabel("Date: " + app.getAppointmentDate()));
                infoPanel.add(new JLabel("Time: " + (app.getAppointmentTime() != null ? app.getAppointmentTime() : "N/A")));
                infoPanel.add(new JLabel("Status: " + app.getStatus()));
                infoPanel.add(new JSeparator());
                infoPanel.add(new JLabel("<html><b>Clinical Notes:</b></html>"));

                panel.add(infoPanel, BorderLayout.NORTH);

                // --- NOTES AREA ---
                String notes = (app.getClinicalNotes() == null || app.getClinicalNotes().isEmpty()) 
                               ? "No clinical notes recorded for this session." : app.getClinicalNotes();

                JTextArea displayNotes = new JTextArea(notes);
                displayNotes.setEditable(false);
                displayNotes.setLineWrap(true);
                displayNotes.setWrapStyleWord(true);
                displayNotes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                displayNotes.setBackground(new Color(248, 249, 250));
                displayNotes.setBorder(new EmptyBorder(10, 10, 10, 10));

                JScrollPane scrollPane = new JScrollPane(displayNotes);
                scrollPane.setBorder(new LineBorder(BORDER_COLOR));
                panel.add(scrollPane, BorderLayout.CENTER);

                // --- ACTION AREA ---
                JPanel footer = new JPanel(new BorderLayout());
                footer.setBackground(Color.WHITE);
                footer.setBorder(new EmptyBorder(10, 0, 0, 0));

                boolean isExpired = app.getStatus().equalsIgnoreCase("Expired");

                if (isExpired) {
                    JButton correctBtn = new JButton("Correct This Record");
                    correctBtn.setBackground(new Color(230, 126, 34));
                    correctBtn.setForeground(Color.WHITE);
                    correctBtn.setFocusPainted(false);
                    correctBtn.setToolTipText("This appointment was auto-expired. Correct it to Completed or No Show.");
                    correctBtn.addActionListener(e -> {
                        Window w = SwingUtilities.getWindowAncestor(panel);
                        if (w != null) w.dispose();
                        openUpdateDialog(app);
                    });
                    footer.add(correctBtn, BorderLayout.CENTER);
                } else {
                    JButton updateBtn = new JButton("Modify Record");
                    updateBtn.setBackground(SUCCESS);
                    updateBtn.setForeground(Color.WHITE);
                    updateBtn.setFocusPainted(false);
                    updateBtn.addActionListener(e -> {
                        Window w = SwingUtilities.getWindowAncestor(panel);
                        if (w != null) w.dispose();
                        openUpdateDialog(app);
                    });
                    footer.add(updateBtn, BorderLayout.CENTER);
                }

                panel.add(footer, BorderLayout.SOUTH);

                JOptionPane.showMessageDialog(this, panel, "Record Details", JOptionPane.PLAIN_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void openUpdateDialog(Appointment app) {
        JPanel editPanel = new JPanel(new BorderLayout(10, 10));
        editPanel.setPreferredSize(new Dimension(400, 300));

        String[] statuses = {"Completed", "No Show", "Cancelled"};
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        statusBox.setSelectedItem(app.getStatus().equalsIgnoreCase("Expired") ? "Completed" : app.getStatus());

        JTextArea notesArea = new JTextArea(app.getClinicalNotes());
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(notesArea);

        JPanel top = new JPanel(new GridLayout(0, 1, 5, 5));
        if (app.getStatus().equalsIgnoreCase("Expired")) {
            JLabel warningLbl = new JLabel("<html><font color='#c0392b'>⚠ This appointment was auto-expired.<br>Please correct the status.</font></html>");
            top.add(warningLbl);
        }
        top.add(new JLabel("Update Record Status:"));
        top.add(statusBox);
        top.add(new JLabel("Clinical Notes & Observations:"));
        
        editPanel.add(top, BorderLayout.NORTH);
        editPanel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, editPanel, 
                     "Update Treatment Record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String newStatus = (String) statusBox.getSelectedItem();
                String newNotes = notesArea.getText();

                boolean success = appointmentController.updateTreatmentRecord(app.getAppointmentId(), newStatus, newNotes);

                if (success) {
                    app.setStatus(newStatus);
                    app.setClinicalNotes(newNotes);
                    JOptionPane.showMessageDialog(this, "Record successfully updated.");
                    HISTORY_CACHE.remove(CACHE_KEY);
                    loadHistoryData(true);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving record: " + ex.getMessage());
            }
        }
    }
}
