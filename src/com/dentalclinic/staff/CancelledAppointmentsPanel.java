package com.dentalclinic.staff;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;

public class CancelledAppointmentsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();

    public CancelledAppointmentsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- HEADER ---
        JLabel title = new JLabel("Cancellation History");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // --- TABLE ---
        String[] columns = {"ID", "Patient Name", "Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Double click just to see details (Read-Only)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showReadOnlyDetails();
                }
            }
        });

        loadCancelledData();
    }

    public void loadCancelledData() {
        try {
            model.setRowCount(0);
            // Now fetching the array that contains the pre-formatted Full Name
            List<Object[]> list = appService.getCancelledRequestsWithNames(); 

            for (Object[] row : list) {
                model.addRow(row); // 'row' already contains [ID, Name, Service, Date, Time, Status]
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showReadOnlyDetails() {
        JOptionPane.showMessageDialog(this, 
            "This is a cancelled record for historical purposes.\nTo book this patient again, they must create a new request.", 
            "Record Information", 
            JOptionPane.INFORMATION_MESSAGE);
    }
}