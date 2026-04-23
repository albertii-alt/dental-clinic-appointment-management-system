package com.dentalclinic.view.patient;

import com.dentalclinic.controller.AppointmentController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.dentalclinic.model.Appointment;

public class PatientHistoryPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final Map<Integer, CacheEntry> HISTORY_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private final int patientID;
    private final JLabel subtitle;
    private SwingWorker<List<Appointment>, Void> loadWorker;
    private long loadRequestId = 0;
    private List<Appointment> completedHistory = new ArrayList<>();

    // THEME CONSTANTS
    private final Color BG = new Color(245, 247, 250);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color TEXT_MAIN = new Color(44, 62, 80);

    private static class CacheEntry {
        private final List<Appointment> data;
        private final long createdAtMs;

        private CacheEntry(List<Appointment> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    public PatientHistoryPanel(int patientID) {
        this.patientID = patientID;
        setLayout(new BorderLayout(20, 20));
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("My Treatment History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(PRIMARY);

        subtitle = new JLabel("Double-click any record to view doctor's notes and clinical feedback.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { HISTORY_CACHE.remove(patientID); loadHistoryDataAsync(); });

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(refreshBtn, BorderLayout.EAST);
        headerPanel.add(titleRow, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Visit Date", "Treatment/Service", "Time Slot", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(model);
        styleTable(table);
        
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showHistoryDetail();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        add(scrollPane, BorderLayout.CENTER);

        loadHistoryData();
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

        // Center and Color the Status Column
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(SUCCESS);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void loadHistoryData() {
        CacheEntry cached = HISTORY_CACHE.get(patientID);
        if (cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            renderHistory(cached.data);
            return;
        }

        loadHistoryDataAsync();
    }

    private void loadHistoryDataAsync() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        final long requestId = ++loadRequestId;
        subtitle.setText("Loading treatment history...");
        table.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        loadWorker = new SwingWorker<List<Appointment>, Void>() {
            @Override
            protected List<Appointment> doInBackground() throws Exception {
                List<Appointment> all = appointmentController.getAppointmentsByPatient(patientID);
                return all.stream()
                        .filter(a -> a.getStatus().equalsIgnoreCase("Completed"))
                        .collect(Collectors.toList());
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Appointment> data = get();
                    HISTORY_CACHE.put(patientID, new CacheEntry(new ArrayList<>(data), System.currentTimeMillis()));
                    renderHistory(data);
                } catch (Exception ex) {
                    subtitle.setText("Failed to load treatment history.");
                    JOptionPane.showMessageDialog(PatientHistoryPanel.this,
                            "Unable to load treatment history: " + ex.getMessage(),
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

    private void renderHistory(List<Appointment> data) {
        completedHistory = new ArrayList<>(data);
        model.setRowCount(0);

        if (completedHistory.isEmpty()) {
            subtitle.setText("No completed treatments found in your history.");
            return;
        }

        subtitle.setText("Double-click any record to view doctor's notes and clinical feedback.");
        for (Appointment a : completedHistory) {
            model.addRow(new Object[]{
                a.getAppointmentDate(),
                a.getServiceType(),
                a.getAppointmentTime(),
                "COMPLETED"
            });
        }
    }

    private void showHistoryDetail() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        try {
            if (row >= completedHistory.size()) {
                return;
            }
            Appointment app = completedHistory.get(row);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new EmptyBorder(10, 10, 10, 10));
            panel.setBackground(Color.WHITE);

            // Summary Header
            String headerHTML = "<html><div style='font-family: Segoe UI;'>" +
                "<b style='color:#2980b9; font-size:14px;'>TREATMENT SUMMARY</b><br>" +
                "Date: " + app.getAppointmentDate() + " | " + app.getServiceType() +
                "</div></html>";
            JLabel headerLbl = new JLabel(headerHTML);
            panel.add(headerLbl);
            panel.add(Box.createVerticalStrut(15));

            // Notes Section
            JLabel notesTitle = new JLabel("Dentist's Clinical Feedback:");
            notesTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            panel.add(notesTitle);
            panel.add(Box.createVerticalStrut(5));

            String notes = (app.getClinicalNotes() == null || app.getClinicalNotes().trim().isEmpty()) 
                           ? "The dentist has not recorded any specific notes for this visit." : app.getClinicalNotes();
            
            JTextArea notesArea = new JTextArea(notes);
            notesArea.setEditable(false);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            notesArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
            notesArea.setBackground(new Color(248, 249, 250));
            notesArea.setBorder(new EmptyBorder(10, 10, 10, 10));

            JScrollPane scroll = new JScrollPane(notesArea);
            scroll.setPreferredSize(new Dimension(420, 180));
            scroll.setBorder(new LineBorder(new Color(230, 230, 230)));
            
            panel.add(scroll);

            JOptionPane.showMessageDialog(this, panel, "Clinical Treatment Record", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        }
    }
}
