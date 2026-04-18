package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.model.Appointment;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class PatientHistoryPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private final AppointmentController appointmentController = new AppointmentController();
    
    private boolean isDentist;

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public PatientHistoryPanel(boolean isDentist) {
        this.isDentist = isDentist;
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

        // --- HEADER & SEARCH AREA ---
        JPanel headerArea = new JPanel(new BorderLayout());
        headerArea.setBackground(CARD);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(CARD);
        JLabel title = new JLabel("Patient Treatment History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(PRIMARY);
        JLabel subtitle = new JLabel("Review past procedures and clinical notes.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        // Modern Search Bar
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchBox.setBackground(CARD);
        JLabel searchIcon = new JLabel("Search Name: ");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(250, 35));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1),
                new EmptyBorder(0, 10, 0, 10)
        ));
        
        searchBox.add(searchIcon);
        searchBox.add(searchField);

        headerArea.add(titlePanel, BorderLayout.WEST);
        headerArea.add(searchBox, BorderLayout.EAST);
        cardContainer.add(headerArea, BorderLayout.NORTH);

        // --- TABLE AREA ---
        String[] columns = {"ID", "Patient Name", "Service Performed", "Date", "Time"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        styleTable(table);
        
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        // --- LISTENERS ---
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { showFullHistoryDetail(); }
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, 1));
            }
        });

        add(cardContainer, BorderLayout.CENTER);
        loadHistoryData();
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
        header.setPreferredSize(new Dimension(0, 40));
    }

    public void loadHistoryData() {
        try {
            model.setRowCount(0);
            List<Object[]> data = appointmentController.getTreatmentHistory();
            if (data.isEmpty()) {   
                // If empty, the table remains empty; standard behavior
            } else {
                for (Object[] row : data) {
                    model.addRow(row);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void showFullHistoryDetail() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int modelRow = table.convertRowIndexToModel(row);
        int appId = (int) model.getValueAt(modelRow, 0);
        String patientName = (String) model.getValueAt(modelRow, 1);

        try {
            List<Appointment> allApps = appointmentController.getAllAppointments();
            Appointment app = allApps.stream()
                                     .filter(a -> a.getAppointmentId() == appId)
                                     .findFirst().orElse(null);

            if (app != null) {
                JPanel panel = new JPanel(new BorderLayout(15, 15));
                panel.setPreferredSize(new Dimension(450, 400));
                panel.setBackground(Color.WHITE);

                // --- HEADER INFO ---
                JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                infoPanel.setBackground(Color.WHITE);
                infoPanel.add(new JLabel("<html><b style='color:#2980b9; font-size:16px;'>TREATMENT RECORD #" + appId + "</b></html>"));
                infoPanel.add(new JSeparator());
                infoPanel.add(new JLabel("Patient: " + patientName));
                infoPanel.add(new JLabel("Service: " + app.getServiceType()));
                infoPanel.add(new JLabel("Date: " + app.getAppointmentDate()));
                infoPanel.add(new JLabel("Status: " + app.getStatus()));
                infoPanel.add(new JSeparator());
                infoPanel.add(new JLabel("<html><b>Clinical Notes:</b></html>"));

                panel.add(infoPanel, BorderLayout.NORTH);

                // --- NOTES AREA ---
                String notes = (app.getClinicalNotes() == null || app.getClinicalNotes().isEmpty()) 
                               ? "No clinical notes recorded for this session." : app.getClinicalNotes();

                JTextArea displayNotes = new JTextArea(notes);
                displayNotes.setEditable(false);
                displayNotes.setLineWrap(true);
                displayNotes.setWrapStyleWord(true);
                displayNotes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                displayNotes.setBackground(new Color(248, 249, 250));
                displayNotes.setBorder(new EmptyBorder(10, 10, 10, 10));

                JScrollPane scrollPane = new JScrollPane(displayNotes);
                scrollPane.setBorder(new LineBorder(BORDER_COLOR));
                panel.add(scrollPane, BorderLayout.CENTER);

                // --- ACTION AREA ---
                JPanel footer = new JPanel(new BorderLayout());
                footer.setBackground(Color.WHITE);
                footer.setBorder(new EmptyBorder(10, 0, 0, 0));

                if (isDentist) {
                    JButton updateBtn = new JButton("Modify Record");
                    updateBtn.setBackground(SUCCESS);
                    updateBtn.setForeground(Color.WHITE);
                    updateBtn.setFocusPainted(false);
                    updateBtn.addActionListener(e -> {
                        Window w = SwingUtilities.getWindowAncestor(panel);
                        if (w != null) w.dispose();
                        openUpdateDialog(app);
                    });
                    footer.add(updateBtn, BorderLayout.CENTER);
                } else {
                    JLabel readOnly = new JLabel("Administrator View (Read-Only)", SwingConstants.CENTER);
                    readOnly.setForeground(Color.GRAY);
                    readOnly.setFont(new Font("Segoe UI", Font.ITALIC, 11));
                    footer.add(readOnly, BorderLayout.CENTER);
                }

                panel.add(footer, BorderLayout.SOUTH);

                JOptionPane.showMessageDialog(this, panel, "Record Details", JOptionPane.PLAIN_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void openUpdateDialog(Appointment app) {
        JPanel editPanel = new JPanel(new BorderLayout(10, 10));
        editPanel.setPreferredSize(new Dimension(400, 300));

        String[] statuses = {"Completed", "Cancelled", "Follow-up Required"};
        JComboBox<String> statusBox = new JComboBox<>(statuses);
        statusBox.setSelectedItem(app.getStatus());

        JTextArea notesArea = new JTextArea(app.getClinicalNotes());
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(notesArea);

        JPanel top = new JPanel(new GridLayout(0, 1, 5, 5));
        top.add(new JLabel("Update Record Status:"));
        top.add(statusBox);
        top.add(new JLabel("Clinical Notes & Observations:"));
        
        editPanel.add(top, BorderLayout.NORTH);
        editPanel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, editPanel, 
                     "Update Treatment Record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String newStatus = (String) statusBox.getSelectedItem();
                String newNotes = notesArea.getText();

                boolean success = appointmentController.updateTreatmentRecord(app.getAppointmentId(), newStatus, newNotes);

                if (success) {
                    app.setStatus(newStatus);
                    app.setClinicalNotes(newNotes);
                    JOptionPane.showMessageDialog(this, "Record successfully updated.");
                    loadHistoryData();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving record: " + ex.getMessage());
            }
        }
    }
}
