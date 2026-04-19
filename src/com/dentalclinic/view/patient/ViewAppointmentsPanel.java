package com.dentalclinic.view.patient;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.PatientController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.dentalclinic.model.Appointment;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ViewAppointmentsPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final Map<Integer, CacheEntry> REQUEST_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private final PatientController patientController = new PatientController();
    private List<Appointment> filteredList = new ArrayList<>();
    private final int patientID;
    private SwingWorker<List<Appointment>, Void> loadWorker;
    private long loadRequestId = 0;

    // UI Style Constants
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color PENDING_ORANGE = new Color(230, 126, 34);
    private final Color DECLINED_RED = new Color(231, 76, 60);

    private static class CacheEntry {
        private final List<Appointment> data;
        private final long createdAtMs;

        private CacheEntry(List<Appointment> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    public ViewAppointmentsPanel(int patientID) {
        this.patientID = patientID;
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(25, 40, 25, 40));

        // --- TITLE ---
        JLabel title = new JLabel("My Appointment Requests");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        add(title, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Service", "Date", "Time", "Status"};
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
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        showAppointmentDetails(row, ViewAppointmentsPanel.this.patientID);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(230, 230, 230)));
        add(scrollPane, BorderLayout.CENTER);

        loadData(false);
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

        // Status Renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = (value != null) ? value.toString() : "";
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                
                if (status.equalsIgnoreCase("Pending")) l.setForeground(PENDING_ORANGE);
                else if (status.equalsIgnoreCase("Declined")) l.setForeground(DECLINED_RED);
                else l.setForeground(PRIMARY_BLUE);
                
                return l;
            }
        });
    }

    private void loadData(boolean forceRefresh) {
        CacheEntry cached = REQUEST_CACHE.get(patientID);
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
                List<Appointment> allAppointments = appointmentController.getPatientAppointmentHistory(patientID);
                return allAppointments.stream()
                    .filter(a -> a.getStatus().equalsIgnoreCase("Pending") ||
                                 a.getStatus().equalsIgnoreCase("Declined"))
                    .collect(Collectors.toList());
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Appointment> data = get();
                    REQUEST_CACHE.put(patientID, new CacheEntry(new ArrayList<>(data), System.currentTimeMillis()));
                    renderRows(data);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ViewAppointmentsPanel.this,
                        "Error loading appointments: " + e.getMessage(),
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
        filteredList = new ArrayList<>(data);

        if (filteredList.isEmpty()) {
            showNoDataScreen();
            return;
        }

        for (Appointment a : filteredList) {
            model.addRow(new Object[]{
                a.getServiceType(),
                a.getAppointmentDate().toString(),
                a.getAppointmentTime(),
                a.getStatus()
            });
        }
    }

    private void invalidateCache() {
        REQUEST_CACHE.remove(patientID);
    }
    
    private void showNoDataScreen() {
        removeAll();
        setLayout(new GridBagLayout());
        JLabel noApp = new JLabel("You haven't booked an appointment yet!");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(new Color(149, 165, 166));
        add(noApp);
        revalidate();
        repaint();
    }
    
    private void showAppointmentDetails(int rowIndex, int pID) {
        try {
            Appointment app = filteredList.get(rowIndex);
            com.dentalclinic.model.Patient p = patientController.getPatientById(pID);

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(Color.WHITE);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

            // Summary Section
            JLabel title1 = new JLabel("APPOINTMENT SUMMARY");
            title1.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title1.setForeground(PRIMARY_BLUE);
            detailPanel.add(title1);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createDetailLabel("Service Type:", app.getServiceType()));
            detailPanel.add(createDetailLabel("Date:", app.getAppointmentDate().toString()));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime()));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            if(app.getStatus().equalsIgnoreCase("Pending")) statusLbl.setForeground(PENDING_ORANGE);
            else statusLbl.setForeground(DECLINED_RED);
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(20));

            // Patient Section
            JLabel title2 = new JLabel("PATIENT INFORMATION");
            title2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            title2.setForeground(PRIMARY_BLUE);
            detailPanel.add(title2);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            
            String fullName = p.getFirstName() + " " + (p.getMiddleName().isEmpty() ? "" : p.getMiddleName() + " ") + p.getLastName();
            detailPanel.add(createDetailLabel("Full Name:", fullName));
            detailPanel.add(createDetailLabel("Contact No:", app.getContactAtVisit()));
            detailPanel.add(createDetailLabel("Address:", "<html><p style='width:240px'>" + p.getAddress() + "</p></html>"));

            // Button Logic
            java.util.List<String> optionsList = new java.util.ArrayList<>();
            if (app.getStatus().equalsIgnoreCase("Approved")) optionsList.add("Download Receipt");
            if (app.getStatus().equalsIgnoreCase("Pending")) optionsList.add("Cancel Request");
            if (app.getStatus().equalsIgnoreCase("Declined")) optionsList.add("Delete Record");
            optionsList.add("Close");

            String[] options = optionsList.toArray(new String[0]);
            layoutComponent(detailPanel);

            int selection = JOptionPane.showOptionDialog(
                this, detailPanel, "Request Summary", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[options.length - 1]
            );

            if (selection != -1) {
                String selectedValue = options[selection];
                if (selectedValue.equals("Download Receipt")) {
                    savePanelAsImage(detailPanel, "Receipt_" + app.getAppointmentId());
                } else if (selectedValue.equals("Cancel Request")) {
                    handleCancellation(app, pID);
                }else if (selectedValue.equals("Delete Record")) {
                    // CALL THE NEW DELETE METHOD
                    handleDeleteRecord(app, pID);
                }
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private JLabel createDetailLabel(String title, String value) {
        JLabel label = new JLabel("<html><font color='#7f8c8d'><b>" + title + "</b></font> &nbsp;" + value + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
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
    
    private void savePanelAsImage(JPanel panel, String filename) {
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
                JOptionPane.showMessageDialog(this, "File saved successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
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
                    invalidateCache();
                    loadData(true);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
    
    private void handleDeleteRecord(Appointment app, int pID) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Would you like to remove this declined record from your view?\nThis action cannot be undone.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Assuming your appService has a delete method. 
                // If not, you might need to add deleteAppointment(id) to AppointmentService
                if (appointmentController.deleteAppointment(app.getAppointmentId())) {
                    JOptionPane.showMessageDialog(this, "Record removed.");
                    invalidateCache();
                    loadData(true); // Refresh the table
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete record.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
}
