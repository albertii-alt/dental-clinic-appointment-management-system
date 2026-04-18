package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.PatientController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.UserSession;

public class PendingRequestsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private final PatientController patientController = new PatientController();

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color WARNING = new Color(243, 156, 18); // Modern Orange
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public PendingRequestsPanel() {
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

        // HEADER SECTION
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        
        JLabel title = new JLabel("Pending Appointment Requests");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("Double-click a row to approve or decline new patient bookings.");
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
        
        // Hide logic IDs
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

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        add(cardContainer, BorderLayout.CENTER);
        loadPendingData();
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
        header.setPreferredSize(new Dimension(0, 45));
        
        // Render "Pending" in Orange
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(WARNING);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void loadPendingData() {
        try {
            model.setRowCount(0);
            List<Object[]> data = appointmentController.getPendingRequestsWithNames();
            
            if (data.isEmpty()) {   
                // If empty, the table just stays clear
            } else {
                for (Object[] row : data) {
                    model.addRow(row);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showDecisionModal(int appId, int pId) {
        try {
            Patient p = patientController.getPatientById(pId);
            List<Appointment> history = appointmentController.getPatientAppointmentHistory(pId);
            Appointment app = history.stream().filter(a -> a.getAppointmentId() == appId).findFirst().orElse(null);

            if (app == null) return;

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(CARD);
            detailPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            // APPOINTMENT SECTION
            detailPanel.add(createHeaderLabel("APPOINTMENT REQUEST DETAILS"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createCompactLabel("Service Requested: ", app.getServiceType()));
            detailPanel.add(createCompactLabel("Proposed Date: ", app.getAppointmentDate().toString()));
            detailPanel.add(createCompactLabel("Proposed Time: ", app.getAppointmentTime()));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statusLbl.setForeground(WARNING);
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(20));

            // PATIENT SECTION
            detailPanel.add(createHeaderLabel("PATIENT PROFILE"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createCompactLabel("Full Name: ", p.getFirstName() + " " + p.getLastName()));
            detailPanel.add(createCompactLabel("Age at Booking: ", String.valueOf(app.getAgeAtVisit())));
            detailPanel.add(createCompactLabel("Primary Contact: ", app.getContactAtVisit()));
            detailPanel.add(createCompactLabel("Resident Address: ", p.getAddress()));

            String[] options = {"Approve", "Decline", "Close"};
            int choice = JOptionPane.showOptionDialog(
                this, detailPanel, "Intake Review Slip",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[2]
            );

            int actorId = UserSession.getUserId();
            String actorRole = UserSession.getUserRole();

            if (choice == 0) { // Approve
                if (appointmentController.updateAppointmentStatus(appId, "Approved", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Request approved. Patient will be notified.");
                    loadPendingData();
                }
            } else if (choice == 1) { // Decline
                if (appointmentController.updateAppointmentStatus(appId, "Declined", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Request declined.");
                    loadPendingData();
                }
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
}
