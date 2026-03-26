package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;

public class PatientHistoryPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();
    private int patientID;

    public PatientHistoryPanel(int patientID) {
        this.patientID = patientID;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Treatment History");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        String[] columns = {"Date", "Service", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(model);
        table.setRowHeight(30);
        
        // Double-click to see the Dentist's Clinical Notes
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showHistoryDetail();
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        loadHistoryData();
    }

    private void loadHistoryData() {
        try {
            model.setRowCount(0);
            // This calls the DAO. If you removed the 'AND is_archived = FALSE' from the DAO, 
            // 'all' will now contain everything again.
            List<Appointment> all = appService.getPatientHistory(patientID);
            
            // WE ONLY FILTER BY STATUS 'Completed'. 
            // We do NOT check a.isArchived() here.
            List<Appointment> completed = all.stream()
                .filter(a -> a.getStatus().equalsIgnoreCase("Completed"))
                .collect(Collectors.toList());
              if (completed.isEmpty()) {   
                // Optional: Show a message if no appointments today
                setLayout(new GridBagLayout());
                removeAll();
                JLabel noApp = new JLabel("You have no done appointments yet.");
                noApp.setFont(new Font("Arial", Font.BOLD, 18));
                noApp.setForeground(Color.GRAY);
                add(noApp);
            } else {
            for (Appointment a : completed) {
                model.addRow(new Object[]{
                    a.getAppointmentDate(),
                    a.getServiceType(),
                    a.getAppointmentTime(),
                    a.getStatus()
                });
            }}
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showHistoryDetail() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        try {
            List<Appointment> all = appService.getPatientHistory(patientID);
            // Re-filtering to find the exact one matches the table row
            Appointment app = all.stream()
                .filter(a -> a.getStatus().equalsIgnoreCase("Completed"))
                .collect(Collectors.toList()).get(row);

            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Top Info
            String headerText = "<html><b>Date:</b> " + app.getAppointmentDate() + 
                                "<br><b>Service:</b> " + app.getServiceType() + "</html>";
            panel.add(new JLabel(headerText), BorderLayout.NORTH);

            // Clinical Notes with the "Bloat Fix" (JScrollPane)
            String notes = (app.getClinicalNotes() == null || app.getClinicalNotes().isEmpty()) 
                           ? "No clinical notes recorded." : app.getClinicalNotes();
            
            JTextArea notesArea = new JTextArea(notes);
            notesArea.setEditable(false);
            notesArea.setLineWrap(true);
            notesArea.setWrapStyleWord(true);
            notesArea.setBackground(new Color(245, 245, 245));

            JScrollPane scroll = new JScrollPane(notesArea);
            scroll.setPreferredSize(new Dimension(400, 150)); // Fixed size!
            
            panel.add(new JLabel("Dentist's Feedback / Notes:"), BorderLayout.BEFORE_FIRST_LINE);
            panel.add(scroll, BorderLayout.CENTER);

            JOptionPane.showMessageDialog(this, panel, "Treatment Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}