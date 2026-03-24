package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;

public class PatientTodayPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();

    public PatientTodayPanel(int patientID) {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // --- HEADER ---
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        
        JLabel title = new JLabel("Today's Schedule");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        
        JLabel subTitle = new JLabel("Please arrive 15 minutes before your scheduled time.");
        subTitle.setFont(new Font("Arial", Font.ITALIC, 14));
        subTitle.setForeground(new Color(127, 140, 141));
        
        header.add(title);
        header.add(subTitle);
        add(header, BorderLayout.NORTH);

        // --- TABLE ---
        String[] columns = {"Date", "Time Slot", "Service Type", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(40);
        table.setFont(new Font("Arial", Font.PLAIN, 15));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        table.setSelectionBackground(new Color(52, 152, 219));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));
        add(scrollPane, BorderLayout.CENTER);

        // --- LOAD DATA ---
        loadTodayData(patientID);
    }

    private void loadTodayData(int pID) {
        try {
            model.setRowCount(0);
            // Note: You'll need to add this matching method in AppointmentService too!
            List<Appointment> list = appService.getTodaysAppointmentsByPatient(pID);
            
            if (list.isEmpty()) {
                // Optional: Show a message if no appointments today
                setLayout(new GridBagLayout());
                removeAll();
                JLabel noApp = new JLabel("You have no appointments scheduled for today.");
                noApp.setFont(new Font("Arial", Font.BOLD, 18));
                noApp.setForeground(Color.GRAY);
                add(noApp);
            } else {
                for (Appointment a : list) {
                    model.addRow(new Object[]{
                        a.getAppointmentDate(), // Added Date
                        a.getAppointmentTime(),
                        a.getServiceType(),
                        "Confirmed"
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}