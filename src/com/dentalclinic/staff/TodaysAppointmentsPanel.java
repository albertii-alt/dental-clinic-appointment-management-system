package com.dentalclinic.staff;

import com.dentalclinic.model.Appointment;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.service.AppointmentService;

public class TodaysAppointmentsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();

    public TodaysAppointmentsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header with specific Date
        JLabel title = new JLabel("Today's Schedule");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        String[] columns = {"ID", "Patient Name", "Service", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Interaction: Single click to show options for arriving patients
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleArrival();
                }
            }
        });

        loadData();
    }

    public void loadData() {
        try {
            model.setRowCount(0);
            List<Object[]> data = appService.getTodaysSchedule();
            for (Object[] row : data) {
                model.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleArrival() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int appId = (int) model.getValueAt(row, 0);
        String patientName = (String) model.getValueAt(row, 1);

        try {
            List<Appointment> todayList = appService.getTodaysAppointments(); // You have this in Service
            Appointment app = null;
            for(Appointment a : todayList) {
                if(a.getAppointmentId() == appId) {
                    app = a;
                    break;
                }
            }

            if (app == null) return;

            // 2. Get Full Patient Data
            com.dentalclinic.dao.PatientDAO pDao = new com.dentalclinic.dao.PatientDAO();
            com.dentalclinic.model.Patient p = pDao.getPatientById(app.getPatientId());

            // --- UI PANEL (The Modal) ---
            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new GridLayout(0, 1, 5, 5));
            detailPanel.setPreferredSize(new Dimension(450, 400));

            Font boldFont = new Font("Arial", Font.BOLD, 14);
            Font plainFont = new Font("Arial", Font.PLAIN, 14);

            // Appointment Info Header
            JLabel head1 = new JLabel("TODAY'S VISIT DETAILS");
            head1.setFont(new Font("Arial", Font.BOLD, 16));
            head1.setForeground(new Color(41, 128, 185));
            detailPanel.add(head1);
            detailPanel.add(new JSeparator());

            detailPanel.add(createDetailLabel("Scheduled Time:", app.getAppointmentTime(), boldFont));
            detailPanel.add(createDetailLabel("Service Requested:", app.getServiceType(), boldFont));
            detailPanel.add(createDetailLabel("Status:", "CHECK-IN READY", new Color(39, 174, 96), boldFont));

            detailPanel.add(new Box.Filler(new Dimension(0, 10), new Dimension(0, 10), new Dimension(0, 10)));

            // Patient Info Header
            JLabel head2 = new JLabel("PATIENT PROFILE");
            head2.setFont(new Font("Arial", Font.BOLD, 16));
            detailPanel.add(head2);
            detailPanel.add(new JSeparator());

            detailPanel.add(createDetailLabel("Full Name:", patientName, plainFont));
            detailPanel.add(createDetailLabel("Age today:", app.getAgeAtVisit() + " years old", plainFont));
            detailPanel.add(createDetailLabel("Contact No:", app.getContactAtVisit(), plainFont));
            detailPanel.add(createDetailLabel("Address:", "<html>" + p.getAddress() + "</html>", plainFont));

            // --- OPTIONS ---
            String[] options = {"Mark Completed", "No-Show (Cancel)", "Close"};
            int selection = JOptionPane.showOptionDialog(
                this, detailPanel, "Patient Arrival - " + patientName,
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]
            );

            // Get session info
            int actorId = com.dentalclinic.util.UserSession.getUserId();
            String actorRole = com.dentalclinic.util.UserSession.getUserRole();

            if (selection == 0) { // Mark Completed
                // Pass actorId and actorRole
                if (appService.updateAppointmentStatus(appId, "Completed", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment marked as Completed.");
                    loadData();
                }
            } else if (selection == 1) { // No-Show
                int confirm = JOptionPane.showConfirmDialog(this, "Mark as No-Show?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    // Pass actorId and actorRole
                    appService.updateAppointmentStatus(appId, "Cancelled", actorId, actorRole);
                    loadData();
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error loading details: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // Helper for the Labels
    private JLabel createDetailLabel(String title, String value, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> " + value + "</html>");
        label.setFont(font);
        return label;
    }

    // Overloaded helper for colored text
    private JLabel createDetailLabel(String title, String value, Color color, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> <span style='color:rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")'>" + value + "</span></html>");
        label.setFont(font);
        return label;
    }
}