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

public class PatientCancelledPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final Map<Integer, CacheEntry> RECENT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Integer, CacheEntry> ARCHIVE_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private JCheckBox showArchivedBox;
    private final int patientID;
    private SwingWorker<List<Appointment>, Void> loadWorker;
    private long loadRequestId = 0;

    // THEME COLORS
    private final Color BG = new Color(245, 247, 250);
    private final Color DANGER_RED = new Color(192, 57, 43);
    private final Color TEXT_DARK = new Color(44, 62, 80);

    private static class CacheEntry {
        private final List<Appointment> data;
        private final long createdAtMs;

        private CacheEntry(List<Appointment> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    public PatientCancelledPanel(int patientID) {
        this.patientID = patientID;
        setLayout(new BorderLayout(20, 20));
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Cancelled Appointments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(DANGER_RED);
        headerPanel.add(title, BorderLayout.WEST);
        
        // ADDED: ARCHIVE TOGGLE
        showArchivedBox = new JCheckBox("Show Archived (>30 days)");
        showArchivedBox.setOpaque(false);
        showArchivedBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        showArchivedBox.addActionListener(e -> loadCancelledData(false));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(DANGER_RED);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { RECENT_CACHE.remove(patientID); ARCHIVE_CACHE.remove(patientID); loadCancelledData(true); });

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(showArchivedBox);
        eastPanel.add(refreshBtn);
        headerPanel.add(eastPanel, BorderLayout.EAST);

        JLabel subtitle = new JLabel("Records of voided or missed requests");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Service Type", "Original Date", "Original Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        add(scrollPane, BorderLayout.CENTER);

        // --- FOOTER NOTE ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setBackground(new Color(253, 237, 236)); // Very light red tint
        footer.setBorder(new MatteBorder(0, 4, 0, 0, DANGER_RED));
        
        JLabel info = new JLabel(" Note: These records are permanent. Please book a new appointment if you still require dental services.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        info.setForeground(TEXT_DARK);
        footer.add(info);
        
        add(footer, BorderLayout.SOUTH);

        loadCancelledData(false);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(250, 235, 235));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(DANGER_RED);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
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
        header.setPreferredSize(new Dimension(0, 45));

        // Red text for the Status column
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(DANGER_RED);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void loadCancelledData(boolean forceRefresh) {
        Map<Integer, CacheEntry> targetCache = showArchivedBox.isSelected() ? ARCHIVE_CACHE : RECENT_CACHE;
        CacheEntry cached = targetCache.get(patientID);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            renderRows(cached.data);
            return;
        }

        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        final long requestId = ++loadRequestId;
        table.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        loadWorker = new SwingWorker<List<Appointment>, Void>() {
            @Override
            protected List<Appointment> doInBackground() throws Exception {
                if (showArchivedBox.isSelected()) {
                    return appointmentController.getAppointmentsByPatient(patientID).stream()
                        .filter(a -> a.getStatus().equalsIgnoreCase("Cancelled"))
                        .collect(Collectors.toList());
                }
                return appointmentController.getAutoArchivedCancelled(patientID);
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Appointment> data = get();
                    targetCache.put(patientID, new CacheEntry(new ArrayList<>(data), System.currentTimeMillis()));
                    renderRows(data);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PatientCancelledPanel.this,
                            "Error loading cancelled appointments: " + e.getMessage(),
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

    private void renderRows(List<Appointment> cancelledList) {
        model.setRowCount(0);
        if (cancelledList.isEmpty()) {
            if (showArchivedBox.isSelected()) {
                JOptionPane.showMessageDialog(this, "No archived records found.");
            }
            return;
        }

        for (Appointment a : cancelledList) {
            model.addRow(new Object[]{
                a.getServiceType(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                "CANCELLED"
            });
        }
    }

    private void showEmptyState() {
        // Switch to centered message if no data exists
        removeAll();
        setLayout(new GridBagLayout());
        JLabel noApp = new JLabel("You have no cancelled appointments record.");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(Color.LIGHT_GRAY);
        add(noApp);
        revalidate();
        repaint();
    }
}
