package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
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
    private List<Appointment> todayList = new ArrayList<>();

    // UI Constants
    private final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private final Color BG_COLOR = new Color(245, 247, 250);
    private final Color TEXT_DARK = new Color(44, 62, 80);

    public PatientTodayPanel(int patientID) {
        // --- PANEL SETUP ---
        setLayout(new BorderLayout(20, 20));
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- HEADER ---
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        
        JLabel title = new JLabel("Today's Schedule");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_DARK);
        
        JLabel subTitle = new JLabel("Please arrive 15 minutes before your scheduled time.");
        subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subTitle.setForeground(new Color(127, 140, 141));
        
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subTitle);
        add(header, BorderLayout.NORTH);

        // --- TABLE ---
        String[] columns = {"Date", "Time Slot", "Service Type", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);

        // Interaction
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
        scrollPane.setBorder(new LineBorder(new Color(230, 230, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        loadTodayData(patientID);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setGridColor(new Color(240, 240, 240));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(Color.BLACK);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(Color.WHITE);
        header.setForeground(TEXT_DARK);
        header.setPreferredSize(new Dimension(0, 40));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Custom Status Renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setFont(l.getFont().deriveFont(Font.BOLD));
                l.setForeground(PRIMARY_COLOR);
                return l;
            }
        });
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
                        a.getStatus().toUpperCase()
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
            detailPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

            // Visit Summary Header
            JLabel summaryTitle = new JLabel("VISIT SUMMARY");
            summaryTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            summaryTitle.setForeground(PRIMARY_COLOR);
            detailPanel.add(summaryTitle);
            detailPanel.add(Box.createVerticalStrut(8));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(12));

            detailPanel.add(createDetailLabel("Service Type:", app.getServiceType()));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime()));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            statusLbl.setForeground(new Color(39, 174, 96)); // Green for active today
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(25));

            // Patient Info Header
            JLabel patientTitle = new JLabel("PATIENT DETAILS");
            patientTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
            patientTitle.setForeground(PRIMARY_COLOR);
            detailPanel.add(patientTitle);
            detailPanel.add(Box.createVerticalStrut(8));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(12));

            String fullName = p.getFirstName() + " " + p.getLastName();
            detailPanel.add(createDetailLabel("Full Name:", fullName));
            detailPanel.add(createDetailLabel("Contact No:", app.getContactAtVisit()));
            detailPanel.add(createDetailLabel("Registered Address:", "<html><p style='width:220px'>" + p.getAddress() + "</p></html>"));

            UIManager.put("Button.background", Color.WHITE);
            JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Details", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Close"}, "Close"
            );

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving details: " + ex.getMessage());
        }
    }

    private JLabel createDetailLabel(String title, String value) {
        JLabel label = new JLabel("<html><font color='#7f8c8d'><b>" + title + "</b></font> &nbsp;" + value + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setBorder(new EmptyBorder(4, 0, 4, 0));
        return label;
    }

    private void showNoDataMessage() {
        removeAll();
        setLayout(new GridBagLayout());
        
        JPanel centerPnl = new JPanel();
        centerPnl.setLayout(new BoxLayout(centerPnl, BoxLayout.Y_AXIS));
        centerPnl.setOpaque(false);

        JLabel noApp = new JLabel("You have no appointments scheduled for today.");
        noApp.setFont(new Font("Segoe UI", Font.BOLD, 18));
        noApp.setForeground(new Color(189, 195, 199));
        noApp.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerPnl.add(noApp);
        add(centerPnl);
        revalidate();
        repaint();
    }
}