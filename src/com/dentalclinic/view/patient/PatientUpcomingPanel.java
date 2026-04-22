package com.dentalclinic.view.patient;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.PatientController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import com.dentalclinic.model.Appointment;

public class PatientUpcomingPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final Map<Integer, CacheEntry> UPCOMING_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private List<Appointment> upcomingList = new ArrayList<>();
    private List<Appointment> filteredList = new ArrayList<>();
    private final AppointmentController appointmentController = new AppointmentController();
    private final PatientController patientController = new PatientController();
    private final int patientID;
    private SwingWorker<List<Appointment>, Void> loadWorker;
    private long loadRequestId = 0;

    // UI Colors
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SUCCESS_GREEN = new Color(39, 174, 96);
    private final Color BG_LIGHT = new Color(245, 247, 250);

    private static class CacheEntry {
        private final List<Appointment> data;
        private final long createdAtMs;

        private CacheEntry(List<Appointment> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    public PatientUpcomingPanel(int patientID) {
        this.patientID = patientID;
        // --- PANEL SETUP ---
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- HEADER ---
        JLabel title = new JLabel("My Upcoming Appointments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(PRIMARY_BLUE);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { UPCOMING_CACHE.remove(patientID); loadData(true); });

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(refreshBtn, BorderLayout.EAST);
        add(titleRow, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(model);
        styleTable(table);

        // Interaction
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        showApprovedAppointmentDetails(row, PatientUpcomingPanel.this.patientID);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 230, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // --- HINT / FOOTER ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setOpaque(false);
        JLabel hintIcon = new JLabel();
        hintIcon.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.LIGHTBULB, 13, new Color(127, 140, 141)));
        JLabel hint = new JLabel(" Double-click an appointment to view details or download your receipt.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        hint.setForeground(new Color(127, 140, 141));
        footer.add(hintIcon);
        footer.add(hint);
        add(footer, BorderLayout.SOUTH);

        loadData(false);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(Color.BLACK);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 40));

        // Status Column Custom Renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setForeground(SUCCESS_GREEN);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
    }

    private void loadData(boolean forceRefresh) {
        CacheEntry cached = UPCOMING_CACHE.get(patientID);
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
                return appointmentController.getUpcomingScheduleByPatient(patientID);
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Appointment> data = get();
                    UPCOMING_CACHE.put(patientID, new CacheEntry(new ArrayList<>(data), System.currentTimeMillis()));
                    renderRows(data);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PatientUpcomingPanel.this,
                            "Error loading upcoming appointments: " + e.getMessage(),
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

    private void renderRows(List<Appointment> data) {
        model.setRowCount(0);
        upcomingList = new ArrayList<>(data);
        filteredList = new ArrayList<>(upcomingList);

        if (upcomingList.isEmpty()) {
            showNoDataMessage();
            return;
        }

        for (Appointment a : upcomingList) {
            model.addRow(new Object[]{
                a.getServiceType(),
                a.getAppointmentDate(),
                a.getAppointmentTime(),
                "Confirmed"
            });
        }
        revalidate();
        repaint();
    }

    private void showApprovedAppointmentDetails(int rowIndex, int pID) {
        try {
            Appointment app = filteredList.get(rowIndex);
            com.dentalclinic.model.Patient p = patientController.getPatientById(pID);

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(Color.WHITE);
            detailPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

            // Section Headers Styling
            Font sectionFont = new Font("Segoe UI", Font.BOLD, 13);
            Color sectionColor = PRIMARY_BLUE;

            // SECTION 1: SUMMARY
            JLabel title1 = new JLabel("APPOINTMENT SUMMARY");
            title1.setFont(sectionFont);
            title1.setForeground(sectionColor);
            detailPanel.add(title1);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createDetailLabel("Service Type:", app.getServiceType()));
            detailPanel.add(createDetailLabel("Scheduled Date:", app.getAppointmentDate().toString()));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime()));

            JLabel statusLbl = new JLabel("Status: " + "CONFIRMED");
            statusLbl.setForeground(SUCCESS_GREEN);
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            detailPanel.add(statusLbl);
            detailPanel.add(Box.createVerticalStrut(20));

            // SECTION 2: PATIENT
            JLabel title2 = new JLabel("PATIENT INFORMATION");
            title2.setFont(sectionFont);
            title2.setForeground(sectionColor);
            detailPanel.add(title2);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            String fullName = p.getFirstName() + " " + (p.getMiddleName().isEmpty() ? "" : p.getMiddleName() + " ") + p.getLastName();
            detailPanel.add(createDetailLabel("Patient Name:", fullName));
            detailPanel.add(createDetailLabel("Birthdate:", p.getBirthDate().toString()));
            detailPanel.add(createDetailLabel("Contact No:", app.getContactAtVisit()));
            detailPanel.add(createDetailLabel("Address:", "<html><p style='width:240px'>" + p.getAddress() + "</p></html>"));

            // Setup Options
            String[] options = {"Download Receipt", "Close"};
            layoutComponent(detailPanel);

            int selection = JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Receipt", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]
            );

            if (selection == 0) {
                savePanelAsImage(detailPanel, "Receipt_" + app.getAppointmentId());
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private JLabel createDetailLabel(String title, String value) {
        JLabel label = new JLabel("<html><font color='#7f8c8d'><b>" + title + "</b></font> &nbsp;" + value + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setBorder(new EmptyBorder(3, 0, 3, 0));
        return label;
    }

    private void layoutComponent(Component c) {
        synchronized (c.getTreeLock()) {
            c.doLayout();
            if (c instanceof Container) {
                for (Component child : ((Container) c).getComponents()) {
                    layoutComponent(child);
                }
            }
        }
    }
    
    private void savePanelAsImage(JPanel panel) { /* Implementation below */ }

    private void savePanelAsImage(JPanel panel, String filename) {
        panel.setSize(panel.getPreferredSize());
        layoutComponent(panel);
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        panel.printAll(g2d);
        g2d.dispose();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filename + ".png"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(image, "png", fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Receipt saved successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
            }
        }
    }

    private void showNoDataMessage() {
        removeAll();
        setLayout(new GridBagLayout());
        JLabel noApp = new JLabel("You have no upcoming appointments.");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(new Color(189, 195, 199));
        add(noApp);
    }

    private void handleCancellation(Appointment app, int pID) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to cancel this appointment request?", 
            "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int actorId = com.dentalclinic.util.UserSession.getUserId();
                String actorRole = com.dentalclinic.util.UserSession.getUserRole();

                if (appointmentController.updateAppointmentStatus(app.getAppointmentId(), "Cancelled", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment Cancelled Successfully.");
                    UPCOMING_CACHE.remove(patientID);
                    loadData(true);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
