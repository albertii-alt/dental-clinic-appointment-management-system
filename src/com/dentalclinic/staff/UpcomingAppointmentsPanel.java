package com.dentalclinic.staff;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.dao.PatientDAO;
import com.toedter.calendar.JDateChooser;

public class UpcomingAppointmentsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();
    private PatientDAO pDao = new PatientDAO();

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public UpcomingAppointmentsPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- THE MAIN CARD ---
        JPanel cardContainer = new JPanel(new BorderLayout(0, 20));
        cardContainer.setBackground(CARD);
        cardContainer.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // HEADER SECTION
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        
        JLabel title = new JLabel("Confirmed Upcoming Appointments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("Manage and view all approved patient schedules.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setBackground(CARD);
        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        cardContainer.add(header, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"App ID", "Patient ID", "Patient Name", "Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
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

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        add(cardContainer, BorderLayout.CENTER);
        loadUpcomingData();
    }

    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));
        
        // Status Color Renderer
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
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

    private void loadUpcomingData() {
        try {
            model.setRowCount(0);
            List<Appointment> upcoming = appService.getUpcomingAppointments();
            if (upcoming.isEmpty()) {   
                // If empty, the table just shows no rows.
            } else {
                for (Appointment a : upcoming) {
                    Patient p = pDao.getPatientById(a.getPatientId());
                    String fullName = p.getFirstName() + " " + p.getLastName();
                    model.addRow(new Object[]{
                        a.getAppointmentId(), a.getPatientId(), fullName, 
                        a.getServiceType(), a.getAppointmentDate(), a.getAppointmentTime(), a.getStatus()
                    });
                }
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
            detailPanel.setBackground(CARD);
            detailPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            // APPOINTMENT SECTION
            detailPanel.add(createHeaderLabel("APPOINTMENT SUMMARY"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createCompactLabel("Service Type: ", app.getServiceType()));
            detailPanel.add(createCompactLabel("Date: ", app.getAppointmentDate().toString()));
            detailPanel.add(createCompactLabel("Time Slot: ", app.getAppointmentTime()));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statusLbl.setForeground(SUCCESS);
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(20));

            // PATIENT SECTION
            detailPanel.add(createHeaderLabel("PATIENT INFORMATION"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createCompactLabel("Full Name: ", p.getFirstName() + " " + p.getLastName()));
            detailPanel.add(createCompactLabel("Age: ", String.valueOf(app.getAgeAtVisit())));
            detailPanel.add(createCompactLabel("Contact No: ", app.getContactAtVisit()));
            detailPanel.add(createCompactLabel("Full Address: ", p.getAddress()));

            String[] options = {"Reschedule", "Cancel Appointment", "Close"};
            int choice = JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Detail Record",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[2]
            );

            if (choice == 0) { 
                openRescheduleDialog(appId);
            } else if (choice == 1) { 
                handleStaffCancellation(appId);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private JLabel createHeaderLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(PRIMARY);
        return lbl;
    }

    private JLabel createCompactLabel(String title, String value) {
        JLabel label = new JLabel("<html><b style='color:#2c3e50'>" + title + "</b> " + value + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        return label;
    }
    
    private void openRescheduleDialog(int appId) {
        JDialog rescheduleDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Reschedule", true);
        rescheduleDialog.setLayout(new BorderLayout());

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainContainer.setBackground(BG);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Dimension inputSize = new Dimension(300, 40); 

        autoAddLeftLabel(mainContainer, "Select New Date:", labelFont);
        mainContainer.add(Box.createVerticalStrut(5));

        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setPreferredSize(inputSize);
        dateChooser.setMaximumSize(inputSize); 
        dateChooser.setMinSelectableDate(new java.util.Date());
        mainContainer.add(dateChooser);

        mainContainer.add(Box.createVerticalStrut(20));

        autoAddLeftLabel(mainContainer, "Available Time Slots:", labelFont);
        mainContainer.add(Box.createVerticalStrut(5));

        DefaultComboBoxModel<String> timeModel = new DefaultComboBoxModel<>(new String[]{"Pick a date..."});
        JComboBox<String> timeBox = new JComboBox<>(timeModel);
        timeBox.setPreferredSize(inputSize);
        timeBox.setMaximumSize(inputSize);
        timeBox.setEnabled(false);
        mainContainer.add(timeBox);

        mainContainer.add(Box.createVerticalStrut(30));

        JButton confirmBtn = new JButton("Update Schedule");
        confirmBtn.setPreferredSize(new Dimension(300, 45));
        confirmBtn.setMaximumSize(new Dimension(300, 45));
        confirmBtn.setBackground(SUCCESS); 
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mainContainer.add(confirmBtn);

        dateChooser.getDateEditor().addPropertyChangeListener("date", evt -> {
            java.util.Date selected = dateChooser.getDate();
            if (selected != null) {
                try {
                    List<String> available = appService.getAvailableSlotsForDate(selected);
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
            int actorId = com.dentalclinic.util.UserSession.getUserId();
            String actorRole = com.dentalclinic.util.UserSession.getUserRole();

            try {
                if (appService.rescheduleAppointment(appId, sqlDate, selectedTime, actorId, actorRole)) {
                    JOptionPane.showMessageDialog(rescheduleDialog, "Appointment Rescheduled.");
                    rescheduleDialog.dispose();
                    loadUpcomingData();
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        rescheduleDialog.add(mainContainer, BorderLayout.CENTER);
        rescheduleDialog.pack();
        rescheduleDialog.setLocationRelativeTo(this);
        rescheduleDialog.setVisible(true);
    }

    private void autoAddLeftLabel(JPanel container, String text, Font font) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(300, 25));
        JLabel label = new JLabel(text);
        label.setFont(font);
        wrapper.add(label);
        container.add(wrapper);
    }
    
    private void handleStaffCancellation(int appId) {
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Cancel this appointment? This frees up the slot and notifies the patient.", 
            "Confirm Cancellation", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int actorId = com.dentalclinic.util.UserSession.getUserId();
                String actorRole = com.dentalclinic.util.UserSession.getUserRole();
                if (appService.updateAppointmentStatus(appId, "Cancelled", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment Cancelled.");
                    loadUpcomingData(); 
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
}