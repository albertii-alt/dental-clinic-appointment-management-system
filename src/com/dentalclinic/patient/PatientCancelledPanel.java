package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;

public class PatientCancelledPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();

    public PatientCancelledPanel(int patientID) {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- HEADER ---
        JLabel title = new JLabel("Cancelled Appointments");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(192, 57, 43)); // Dark Red for "Cancelled" theme
        add(title, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Service Type", "Original Date", "Original Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // --- FOOTER NOTE ---
        JLabel info = new JLabel("Note: Cancelled appointments cannot be restored. Please book a new one if needed.");
        info.setFont(new Font("Arial", Font.ITALIC, 12));
        add(info, BorderLayout.SOUTH);

        loadCancelledData(patientID);
    }

    private void loadCancelledData(int pID) {
        try {
            model.setRowCount(0);
            List<Appointment> list = appService.getPatientHistory(pID);
                if (list.isEmpty()) {   
                // Optional: Show a message if no appointments today
                setLayout(new GridBagLayout());
                removeAll();
                JLabel noApp = new JLabel("You have no cancelled appointments!");
                noApp.setFont(new Font("Arial", Font.BOLD, 18));
                noApp.setForeground(Color.GRAY);
                add(noApp);
            } else {
            for (Appointment a : list) {
                if (a.getStatus().equalsIgnoreCase("Cancelled")) {
                    model.addRow(new Object[]{
                        a.getServiceType(),
                        a.getAppointmentDate(),
                        a.getAppointmentTime(),
                        "Cancelled"
                    });
                }
            }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}