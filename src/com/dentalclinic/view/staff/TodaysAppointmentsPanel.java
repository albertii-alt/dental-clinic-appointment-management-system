package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.PatientController;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.UserSession;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TodaysAppointmentsPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final String CACHE_KEY = "TODAY_SCHEDULE";
    private static final Map<String, CacheEntry> TODAY_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private final PatientController patientController = new PatientController();
    private SwingWorker<List<Object[]>, Void> loadWorker;
    private long loadRequestId = 0;

    private static class CacheEntry {
        private final List<Object[]> data;
        private final long createdAtMs;

        private CacheEntry(List<Object[]> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    // THEME CONSTANTS
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color DANGER = new Color(192, 57, 43);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public TodaysAppointmentsPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- MAIN CONTAINER ---
        JPanel mainCard = new JPanel(new BorderLayout(0, 20));
        mainCard.setBackground(CARD);
        mainCard.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // HEADER
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        
        JLabel title = new JLabel("Today's Patient Schedule");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(PRIMARY);
        
        JLabel dateLabel = new JLabel("Operating Date: " + java.time.LocalDate.now().toString());
        dateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        dateLabel.setForeground(Color.GRAY);

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setBackground(CARD);
        titleBox.add(title);
        titleBox.add(dateLabel);
        header.add(titleBox, BorderLayout.WEST);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { invalidateCache(); loadData(true); });
        header.add(refreshBtn, BorderLayout.EAST);

        mainCard.add(header, BorderLayout.NORTH);

        // --- TABLE ---
        String[] columns = {"ID", "Patient Name", "Service", "Scheduled Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        // Hide ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleArrival();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER_COLOR, 1));
        mainCard.add(scroll, BorderLayout.CENTER);

        add(mainCard, BorderLayout.CENTER);
        loadData(false);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45); // Taller rows for better "Live" visibility
        table.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 15));
        header.setPreferredSize(new Dimension(0, 50));
        
        // Center-align the Time column for readability
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
    }

    public void loadData() {
        loadData(false);
    }

    private void loadData(boolean forceRefresh) {
        CacheEntry cached = TODAY_CACHE.get(CACHE_KEY);
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

        loadWorker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return appointmentController.getTodaysSchedule();
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Object[]> data = get();
                    TODAY_CACHE.put(CACHE_KEY, new CacheEntry(new ArrayList<>(data), System.currentTimeMillis()));
                    renderRows(data);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(TodaysAppointmentsPanel.this,
                            "Error loading today's schedule: " + e.getMessage(),
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

    private void renderRows(List<Object[]> data) {
        model.setRowCount(0);
        if (data.isEmpty()) {
            showEmptyState();
            return;
        }

        for (Object[] row : data) {
            model.addRow(row);
        }
    }

    private void invalidateCache() {
        TODAY_CACHE.remove(CACHE_KEY);
    }

    private void showEmptyState() {
        setLayout(new GridBagLayout());
        removeAll();
        JLabel noApp = new JLabel("No patients scheduled for today.");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(Color.LIGHT_GRAY);
        add(noApp);
        revalidate();
        repaint();
    }

    private void handleArrival() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int appId = (int) model.getValueAt(row, 0);
        String patientName = (String) model.getValueAt(row, 1);

        try {
            List<Appointment> todayList = appointmentController.getTodaysAppointments();
            Appointment app = todayList.stream().filter(a -> a.getAppointmentId() == appId).findFirst().orElse(null);
            if (app == null) return;

            Patient p = patientController.getPatientById(app.getPatientId());

            // --- THE ARRIVAL SLIP UI ---
            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(CARD);
            detailPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            detailPanel.add(createHeaderLabel("VISIT SUMMARY"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime(), SUCCESS));
            detailPanel.add(createDetailLabel("Service:", app.getServiceType(), PRIMARY));
            
            detailPanel.add(Box.createVerticalStrut(20));

            detailPanel.add(createHeaderLabel("PATIENT PROFILE"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createDetailLabel("Full Name:", patientName, Color.BLACK));
            detailPanel.add(createDetailLabel("Current Age:", app.getAgeAtVisit() + " yrs", Color.BLACK));
            detailPanel.add(createDetailLabel("Contact:", app.getContactAtVisit(), Color.BLACK));
            detailPanel.add(createDetailLabel("Address:", "<html><body style='width: 250px'>" + p.getAddress() + "</body></html>", Color.BLACK));

            String[] options = {"Complete Visit", "Mark No-Show", "Close"};
            int selection = JOptionPane.showOptionDialog(
                this, detailPanel, "Patient Check-In: " + patientName,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]
            );

            int actorId = UserSession.getUserId();
            String actorRole = UserSession.getUserRole();

            if (selection == 0) { // Mark Completed
                if (appointmentController.updateAppointmentStatus(appId, "Completed", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Visit logged as Completed.");
                    invalidateCache();
                    loadData(true);
                }
            } else if (selection == 1) { // No-Show
                int confirm = JOptionPane.showConfirmDialog(this, "Confirm No-Show? (Patient record will reflect cancellation)", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    appointmentController.updateAppointmentStatus(appId, "Cancelled", actorId, actorRole);
                    invalidateCache();
                    loadData(true);
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private JLabel createHeaderLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(127, 140, 141));
        return lbl;
    }

    private JLabel createDetailLabel(String title, String value, Color color) {
        JLabel label = new JLabel("<html><b>" + title + "</b> <span style='color:rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")'>" + value + "</span></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        return label;
    }
}
