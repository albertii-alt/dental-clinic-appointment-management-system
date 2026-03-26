package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.dao.PatientDAO;

public class PatientTodayPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();
    private List<Appointment> todayList = new ArrayList<>(); // To track the actual objects

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

        // Interaction: Double Click to view details
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        showTodayDetails(row, patientID);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));
        add(scrollPane, BorderLayout.CENTER);

        loadTodayData(patientID);
    }

    private void loadTodayData(int pID) {
        try {
            model.setRowCount(0);
            todayList = appService.getTodaysAppointmentsByPatient(pID);
            
            if (todayList.isEmpty()) {   
                showNoDataMessage();
            } else {
                for (Appointment a : todayList) {
                    model.addRow(new Object[]{
                        a.getAppointmentDate(),
                        a.getAppointmentTime(),
                        a.getServiceType(),
                        a.getStatus() // Usually "Approved" for today
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showTodayDetails(int rowIndex, int pID) {
        try {
            Appointment app = todayList.get(rowIndex);
            PatientDAO pDao = new PatientDAO();
            com.dentalclinic.model.Patient p = pDao.getPatientById(pID);

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(Color.WHITE);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            Font headerFont = new Font("Arial", Font.BOLD, 16);
            Font dataFont = new Font("Arial", Font.PLAIN, 14);

            // SECTION: SUMMARY
            detailPanel.add(new JLabel("<html><b style='font-size:12px;'>VISIT SUMMARY</b></html>"));
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createDetailLabel("Service:", app.getServiceType(), dataFont));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime(), dataFont));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            statusLbl.setForeground(new Color(41, 128, 185)); // Professional Blue
            statusLbl.setFont(headerFont);
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(20));

            // SECTION: PATIENT INFO
            detailPanel.add(new JLabel("<html><b style='font-size:12px;'>PATIENT DETAILS</b></html>"));
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            String fullName = p.getFirstName() + " " + p.getLastName();
            detailPanel.add(createDetailLabel("Name:", fullName, dataFont));
            detailPanel.add(createDetailLabel("Contact:", app.getContactAtVisit(), dataFont));
            detailPanel.add(createDetailLabel("Address:", "<html><p style='width:200px'>" + p.getAddress() + "</p></html>", dataFont));

            JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Details", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Close"}, "Close"
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private JLabel createDetailLabel(String title, String value, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> " + value + "</html>");
        label.setFont(font);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return label;
    }

    private void showNoDataMessage() {
        removeAll();
        setLayout(new GridBagLayout());
        JLabel noApp = new JLabel("You have no appointments scheduled for today.");
        noApp.setFont(new Font("Arial", Font.BOLD, 18));
        noApp.setForeground(Color.GRAY);
        add(noApp);
        revalidate();
        repaint();
    }
}