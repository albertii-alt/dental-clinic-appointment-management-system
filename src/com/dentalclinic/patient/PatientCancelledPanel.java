package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;

public class PatientCancelledPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();

    // THEME COLORS
    private final Color BG = new Color(245, 247, 250);
    private final Color DANGER_RED = new Color(192, 57, 43);
    private final Color TEXT_DARK = new Color(44, 62, 80);

    public PatientCancelledPanel(int patientID) {
        setLayout(new BorderLayout(20, 20));
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Cancelled Appointments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(DANGER_RED);
        headerPanel.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Records of voided or missed requests");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        headerPanel.add(subtitle, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"Service Type", "Original Date", "Original Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        add(scrollPane, BorderLayout.CENTER);

        // --- FOOTER NOTE ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setBackground(new Color(253, 237, 236)); // Very light red tint
        footer.setBorder(new MatteBorder(0, 4, 0, 0, DANGER_RED));
        
        JLabel info = new JLabel(" Note: These records are permanent. Please book a new appointment if you still require dental services.");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        info.setForeground(TEXT_DARK);
        footer.add(info);
        
        add(footer, BorderLayout.SOUTH);

        loadCancelledData(patientID);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(250, 235, 235));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(DANGER_RED);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));

        // Red text for the Status column
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(DANGER_RED);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void loadCancelledData(int pID) {
        try {
            model.setRowCount(0);
            List<Appointment> allHistory = appService.getPatientHistory(pID);
            
            // Filter only for Cancelled status
            List<Appointment> cancelledList = allHistory.stream()
                .filter(a -> a.getStatus().equalsIgnoreCase("Cancelled"))
                .collect(Collectors.toList());

            if (cancelledList.isEmpty()) {   
                showEmptyState();
            } else {
                for (Appointment a : cancelledList) {
                    model.addRow(new Object[]{
                        a.getServiceType(),
                        a.getAppointmentDate(),
                        a.getAppointmentTime(),
                        "CANCELLED"
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showEmptyState() {
        // Switch to centered message if no data exists
        removeAll();
        setLayout(new GridBagLayout());
        JLabel noApp = new JLabel("You have no cancelled appointments record.");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(Color.LIGHT_GRAY);
        add(noApp);
        revalidate();
        repaint();
    }
}