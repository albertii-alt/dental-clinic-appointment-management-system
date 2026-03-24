package com.dentalclinic.staff;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.dao.PatientDAO;

public class PendingRequestsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();
    private PatientDAO pDao = new PatientDAO();

    public PendingRequestsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Pending Appointment Requests");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Updated Columns: Patient Name, Service, Date, Time, Status
        // Hidden Columns: Index 0 (AppID) and Index 1 (PatientID) for logic
        String[] columns = {"App ID", "Patient ID", "Patient Name", "Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(35);
        
        // Hide ID columns from view but keep them in the model for double-click logic
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        int appId = (int) model.getValueAt(row, 0);
                        int pId = (int) model.getValueAt(row, 1);
                        showDecisionModal(appId, pId);
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        loadPendingData();
    }

    private void loadPendingData() {
        try {
            model.setRowCount(0);
            List<Object[]> data = appService.getPendingRequestsWithNames();
            for (Object[] row : data) {
                model.addRow(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showDecisionModal(int appId, int pId) {
        try {
            Patient p = pDao.getPatientById(pId);
            List<Appointment> history = appService.getPatientAppointmentHistory(pId);
            Appointment app = history.stream().filter(a -> a.getAppointmentId() == appId).findFirst().orElse(null);

            if (app == null) return;

            // Main Panel with zero vertical gaps
            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            // Use a consistent font size for a compact look
            Font headerFont = new Font("Arial", Font.BOLD, 13);
            Font dataFont = new Font("Arial", Font.PLAIN, 13);

            // --- APPOINTMENT SECTION ---
            JLabel appTitle = new JLabel("APPOINTMENT SUMMARY");
            appTitle.setFont(headerFont);
            appTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailPanel.add(appTitle);

            detailPanel.add(Box.createVerticalStrut(2));
            JSeparator sep1 = new JSeparator();
            sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            detailPanel.add(sep1);
            detailPanel.add(Box.createVerticalStrut(10));

            // Adding ID fields for Staff reference
            detailPanel.add(createCompactLabel("Appointment ID: ", String.valueOf(appId), dataFont));
            detailPanel.add(createCompactLabel("Service Type: ", app.getServiceType(), dataFont));
            detailPanel.add(createCompactLabel("Date: ", app.getAppointmentDate().toString(), dataFont));
            detailPanel.add(createCompactLabel("Time Slot: ", app.getAppointmentTime(), dataFont));

            // Status with specific orange color from your image
            JLabel statusLbl = new JLabel("<html><b>Status: </b><font color='#F39C12'>" + app.getStatus().toUpperCase() + "</font></html>");
            statusLbl.setFont(new Font("Arial", Font.BOLD, 14));
            statusLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(5));
            JSeparator sep2 = new JSeparator();
            sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            detailPanel.add(sep2);
            detailPanel.add(Box.createVerticalStrut(15));

            // --- PATIENT SECTION ---
            JLabel patTitle = new JLabel("PATIENT INFORMATION");
            patTitle.setFont(headerFont);
            patTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailPanel.add(patTitle);

            detailPanel.add(Box.createVerticalStrut(2));
            JSeparator sep3 = new JSeparator();
            sep3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            detailPanel.add(sep3);
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createCompactLabel("Patient ID: ", String.valueOf(pId), dataFont));
            detailPanel.add(createCompactLabel("Full Name: ", p.getFirstName() + " " + p.getLastName(), dataFont));
            detailPanel.add(createCompactLabel("Birthdate: ", p.getBirthDate().toString(), dataFont));
            detailPanel.add(createCompactLabel("Age at Booking: ", String.valueOf(app.getAgeAtVisit()), dataFont));
            detailPanel.add(createCompactLabel("Contact No: ", app.getContactAtVisit(), dataFont));
            detailPanel.add(createCompactLabel("Full Address: ", p.getAddress(), dataFont));

            // Show Dialog - JOptionPane will now auto-shrink to fit this content exactly
            String[] options = {"Approve", "Decline", "Close"};
            int choice = JOptionPane.showOptionDialog(this, detailPanel, 
                         "Appointment Request Summary", JOptionPane.DEFAULT_OPTION, 
                         JOptionPane.PLAIN_MESSAGE, null, options, options[2]);

            // 1. Get the current user session info
            int actorId = com.dentalclinic.util.UserSession.getUserId();
            String actorRole = com.dentalclinic.util.UserSession.getUserRole();

            if (choice == 0) { // Approve
                // Pass the actorId and actorRole to trigger the log!
                if (appService.updateAppointmentStatus(appId, "Approved", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment Approved!");
                    loadPendingData();
                }
            } else if (choice == 1) { // Decline
                // Pass the actorId and actorRole to trigger the log!
                if (appService.updateAppointmentStatus(appId, "Declined", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment Declined.");
                    loadPendingData();
                }
            }
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        }
    }

    // Helper for tight vertical alignment
    private JLabel createCompactLabel(String title, String value, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> " + value + "</html>");
        label.setFont(font);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0)); // Only 5px bottom margin
        return label;
    }
}