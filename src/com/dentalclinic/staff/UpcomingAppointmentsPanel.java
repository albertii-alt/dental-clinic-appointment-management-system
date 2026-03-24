package com.dentalclinic.staff;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.dao.PatientDAO;

public class UpcomingAppointmentsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();
    private PatientDAO pDao = new PatientDAO();

    public UpcomingAppointmentsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Confirmed Upcoming Appointments");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Same columns as Pending for consistency
        String[] columns = {"App ID", "Patient ID", "Patient Name", "Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(35);
        
        // Hide IDs
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
                        showUpcomingDetailModal(appId, pId);
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        loadUpcomingData();
    }

    private void loadUpcomingData() {
        try {
            model.setRowCount(0);
            // Reusing the DAO logic but filtering for 'Approved'
            List<Appointment> upcoming = appService.getUpcomingAppointments();
            for (Appointment a : upcoming) {
                // Fetching name for the table (Staff needs to see who is coming)
                Patient p = pDao.getPatientById(a.getPatientId());
                String fullName = p.getFirstName() + " " + p.getLastName();
                
                model.addRow(new Object[]{
                    a.getAppointmentId(), a.getPatientId(), fullName, 
                    a.getServiceType(), a.getAppointmentDate(), a.getAppointmentTime(), a.getStatus()
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showUpcomingDetailModal(int appId, int pId) {
        try {
            Patient p = pDao.getPatientById(pId);
            List<Appointment> history = appService.getPatientAppointmentHistory(pId);
            Appointment app = history.stream().filter(a -> a.getAppointmentId() == appId).findFirst().orElse(null);

            if (app == null) return;

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            Font headerFont = new Font("Arial", Font.BOLD, 13);
            Font dataFont = new Font("Arial", Font.PLAIN, 13);

            // APPOINTMENT SECTION
            JLabel appTitle = new JLabel("APPOINTMENT SUMMARY");
            appTitle.setFont(headerFont);
            detailPanel.add(appTitle);
            detailPanel.add(Box.createVerticalStrut(2));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createCompactLabel("Appointment ID: ", String.valueOf(appId), dataFont));
            detailPanel.add(createCompactLabel("Service Type: ", app.getServiceType(), dataFont));
            detailPanel.add(createCompactLabel("Date: ", app.getAppointmentDate().toString(), dataFont));
            detailPanel.add(createCompactLabel("Time Slot: ", app.getAppointmentTime(), dataFont));
            
            // Status in Green since it's Approved
            JLabel statusLbl = new JLabel("<html><b>Status: </b><font color='#27ae60'>" + app.getStatus().toUpperCase() + "</font></html>");
            statusLbl.setFont(new Font("Arial", Font.BOLD, 14));
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(15));

            // PATIENT SECTION
            JLabel patTitle = new JLabel("PATIENT INFORMATION");
            patTitle.setFont(headerFont);
            detailPanel.add(patTitle);
            detailPanel.add(Box.createVerticalStrut(2));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createCompactLabel("Patient ID: ", String.valueOf(pId), dataFont));
            detailPanel.add(createCompactLabel("Full Name: ", p.getFirstName() + " " + p.getLastName(), dataFont));
            detailPanel.add(createCompactLabel("Birthdate: ", p.getBirthDate().toString(), dataFont));
            detailPanel.add(createCompactLabel("Age at Booking: ", String.valueOf(app.getAgeAtVisit()), dataFont));
            detailPanel.add(createCompactLabel("Contact No: ", app.getContactAtVisit(), dataFont));
            detailPanel.add(createCompactLabel("Full Address: ", p.getAddress(), dataFont));

            // BUTTONS: Reschedule, Cancel Appointment, Close
            String[] options = {"Reschedule", "Cancel Appointment", "Close"};
                int choice = JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Details",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[2]
            );

            if (choice == 0) { 
                if (choice == 0) { // Reschedule
                    openRescheduleDialog(appId);
                }
            }else if (choice == 1) { // Cancel Appointment
                handleStaffCancellation(appId);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private JLabel createCompactLabel(String title, String value, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> " + value + "</html>");
        label.setFont(font);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        return label;
    }
    
private void openRescheduleDialog(int appId) {
    JDialog rescheduleDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Reschedule Appointment", true);
    rescheduleDialog.setLayout(new BorderLayout());

    JPanel mainContainer = new JPanel();
    mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
    mainContainer.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
    mainContainer.setBackground(new Color(236, 240, 241));

    Font labelFont = new Font("Arial", Font.BOLD, 14);
    Dimension inputSize = new Dimension(300, 35); 

    // --- HELPER TO FORCE LABEL TO LEFT ---
    autoAddLeftLabel(mainContainer, "Select New Date:", labelFont);
    mainContainer.add(Box.createVerticalStrut(5));

    com.toedter.calendar.JDateChooser dateChooser = new com.toedter.calendar.JDateChooser();
    dateChooser.setPreferredSize(inputSize);
    dateChooser.setMaximumSize(inputSize); 
    dateChooser.setMinSelectableDate(new java.util.Date());
    dateChooser.setAlignmentX(Component.CENTER_ALIGNMENT); // Forces input to Center
    mainContainer.add(dateChooser);

    mainContainer.add(Box.createVerticalStrut(15));

    autoAddLeftLabel(mainContainer, "Available Time Slots:", labelFont);
    mainContainer.add(Box.createVerticalStrut(5));

    DefaultComboBoxModel<String> timeModel = new DefaultComboBoxModel<>(new String[]{"Choose a date first..."});
    JComboBox<String> timeBox = new JComboBox<>(timeModel);
    timeBox.setPreferredSize(inputSize);
    timeBox.setMaximumSize(inputSize);
    timeBox.setEnabled(false);
    timeBox.setAlignmentX(Component.CENTER_ALIGNMENT); // Forces input to Center
    mainContainer.add(timeBox);

    mainContainer.add(Box.createVerticalStrut(25));

    // --- CONFIRM BUTTON ---
    JButton confirmBtn = new JButton("Update Schedule");
    confirmBtn.setPreferredSize(new Dimension(300, 45));
    confirmBtn.setMaximumSize(new Dimension(300, 45));
    confirmBtn.setBackground(new Color(46, 204, 113)); 
    confirmBtn.setForeground(Color.WHITE);
    confirmBtn.setFont(new Font("Arial", Font.BOLD, 16));
    confirmBtn.setFocusPainted(false);
    confirmBtn.setBorderPainted(false);
    confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT); // Forces Button to Center
    mainContainer.add(confirmBtn);

    // --- LOGIC ---
    dateChooser.getDateEditor().addPropertyChangeListener("date", evt -> {
        java.util.Date selected = dateChooser.getDate();
        if (selected != null) {
            try {
                java.util.List<String> available = appService.getAvailableSlotsForDate(selected);
                timeModel.removeAllElements();
                if (available.isEmpty()) {
                    timeModel.addElement("No slots available");
                    timeBox.setEnabled(false);
                } else {
                    for (String slot : available) timeModel.addElement(slot);
                    timeBox.setEnabled(true);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    });

    confirmBtn.addActionListener(e -> {
        if (dateChooser.getDate() == null || !timeBox.isEnabled()) {
            JOptionPane.showMessageDialog(rescheduleDialog, "Please select a valid date and time.");
            return;
        }

        java.sql.Date sqlDate = new java.sql.Date(dateChooser.getDate().getTime());
        String selectedTime = (String) timeBox.getSelectedItem();

        // >>> ADD THESE LINES HERE <<<
        int actorId = com.dentalclinic.util.UserSession.getUserId();
        String actorRole = com.dentalclinic.util.UserSession.getUserRole();

        try {
            // Update the call to include actorId and actorRole
            if (appService.rescheduleAppointment(appId, sqlDate, selectedTime, actorId, actorRole)) {
                JOptionPane.showMessageDialog(rescheduleDialog, "Rescheduled to " + selectedTime);
                rescheduleDialog.dispose();
                loadUpcomingData();
            }
        } catch (Exception ex) { 
            ex.printStackTrace(); 
        }
    });

    rescheduleDialog.add(mainContainer, BorderLayout.CENTER);
    mainContainer.setMaximumSize(new Dimension(350, 400));
    rescheduleDialog.pack();
    rescheduleDialog.setLocationRelativeTo(this);
    rescheduleDialog.setVisible(true);
}

    // Helper method to ensure the label stays on the left regardless of BoxLayout rules
    private void autoAddLeftLabel(JPanel container, String text, Font font) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(300, 25)); // Matches input width
        JLabel label = new JLabel(text);
        label.setFont(font);
        wrapper.add(label);
        container.add(wrapper);
    }
    
    private void handleStaffCancellation(int appId) {
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Are you sure you want to cancel this appointment?\nThis will notify the patient and free up the time slot.", 
            "Confirm Cancellation", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Get session info
                int actorId = com.dentalclinic.util.UserSession.getUserId();
                String actorRole = com.dentalclinic.util.UserSession.getUserRole();

                // Pass the extra parameters here
                if (appService.updateAppointmentStatus(appId, "Cancelled", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment #" + appId + " has been cancelled.");
                    loadUpcomingData(); 
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}