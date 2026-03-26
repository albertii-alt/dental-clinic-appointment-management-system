package com.dentalclinic.staff;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.service.AppointmentService;

public class CancelledAppointmentsPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private AppointmentService appService = new AppointmentService();

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color DANGER = new Color(231, 76, 60);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public CancelledAppointmentsPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- THE MAIN CARD CONTAINER ---
        JPanel cardContainer = new JPanel(new BorderLayout(0, 20));
        cardContainer.setBackground(CARD);
        cardContainer.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // --- HEADER SECTION ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        
        JLabel title = new JLabel("Cancellation History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("View records of cancelled or declined appointment requests.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setBackground(CARD);
        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        cardContainer.add(header, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columns = {"ID", "Patient Name", "Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        // Double click just to see details (Read-Only)
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showReadOnlyDetails();
                }
            }
        });

        add(cardContainer, BorderLayout.CENTER);
        loadCancelledData();
    }

    private void styleTable(JTable table) {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));
        header.setReorderingAllowed(false);
        
        // Status Column Color (Render Cancelled as Red)
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(DANGER);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    public void loadCancelledData() {
        try {
            model.setRowCount(0);
            List<Object[]> list = appService.getCancelledRequestsWithNames(); 
            
            if (list.isEmpty()) {   
                // If empty, show a centered label in the table area
                table.setVisible(false);
                JLabel noApp = new JLabel("No cancelled records found.", SwingConstants.CENTER);
                noApp.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
                noApp.setForeground(Color.LIGHT_GRAY);
                add(noApp, BorderLayout.SOUTH); // Positioned at bottom or replace center
            } else {
                table.setVisible(true);
                for (Object[] row : list) {
                    model.addRow(row); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showReadOnlyDetails() {
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, 
            "This is a cancelled record for historical purposes.\nTo book this patient again, they must create a new request.", 
            "Record Information", 
            JOptionPane.INFORMATION_MESSAGE);
    }
}