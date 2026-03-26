package com.dentalclinic.staff;

import com.dentalclinic.model.Appointment;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import com.dentalclinic.service.AppointmentService;

public class PatientHistoryPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private AppointmentService appService = new AppointmentService();
    
    // NEW: Role check
    private boolean isDentist;

    // Modified Constructor to accept the role
    public PatientHistoryPanel(boolean isDentist) {
        this.isDentist = isDentist;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TOP SEARCH AREA ---
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Search Treatment History:");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(300, 35));
        
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.NORTH);

        // --- TABLE AREA ---
        String[] columns = {"ID", "Patient Name", "Service Performed", "Date", "Time"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(35);
        
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) { 
                    showFullHistoryDetail();
                }
            }
        });
        
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- SEARCH LISTENER ---
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                sorter.setRowFilter(text.trim().isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, 1));
            }
        });

        loadHistoryData();
    }

    public void loadHistoryData() {
        try {
            model.setRowCount(0);
            List<Object[]> data = appService.getTreatmentHistory();
             if (data.isEmpty()) {   
                // Optional: Show a message if no appointments today
                setLayout(new GridBagLayout());
                removeAll();
                JLabel noApp = new JLabel("No done appointments yet!");
                noApp.setFont(new Font("Arial", Font.BOLD, 18));
                noApp.setForeground(Color.GRAY);
                add(noApp);
            } else {
            for (Object[] row : data) {
                model.addRow(row);
            }}
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void showFullHistoryDetail() {
        int row = table.getSelectedRow();
        if (row == -1) return;

        int modelRow = table.convertRowIndexToModel(row);
        int appId = (int) model.getValueAt(modelRow, 0);
        String patientName = (String) model.getValueAt(modelRow, 1);

        try {
            List<Appointment> allApps = appService.getAllAppointments();
            Appointment app = allApps.stream()
                                     .filter(a -> a.getAppointmentId() == appId)
                                     .findFirst().orElse(null);

            if (app != null) {
                // Using BorderLayout so the ScrollPane can occupy the center properly
                JPanel panel = new JPanel(new BorderLayout(10, 10));
                panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

                // --- HEADER INFO (TOP) ---
                JPanel infoPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                infoPanel.add(new JLabel("<html><b style='color:#2980b9; font-size:14px;'>TREATMENT RECORD</b></html>"));
                infoPanel.add(new JSeparator());
                infoPanel.add(new JLabel("Patient: " + patientName));
                infoPanel.add(new JLabel("Service: " + app.getServiceType()));
                infoPanel.add(new JLabel("Date: " + app.getAppointmentDate()));
                infoPanel.add(new JLabel("Status: " + app.getStatus()));
                infoPanel.add(new JSeparator());
                infoPanel.add(new JLabel("<html><b>Clinical Notes:</b></html>"));

                panel.add(infoPanel, BorderLayout.NORTH);

                // --- NOTES AREA (CENTER) ---
                String notes = (app.getClinicalNotes() == null || app.getClinicalNotes().isEmpty()) 
                               ? "No notes recorded." : app.getClinicalNotes();

                JTextArea displayNotes = new JTextArea(notes);
                displayNotes.setEditable(false);
                displayNotes.setLineWrap(true);
                displayNotes.setWrapStyleWord(true);
                displayNotes.setBackground(new Color(245, 245, 245)); // Slightly different color to distinguish read-only

                JScrollPane scrollPane = new JScrollPane(displayNotes);
                scrollPane.setPreferredSize(new Dimension(400, 150)); // Keeps the window consistent!
                panel.add(scrollPane, BorderLayout.CENTER);

                // --- ACTION AREA (BOTTOM) ---
                JPanel actionPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                if (isDentist) {
                    JButton updateBtn = new JButton("Update Clinical Notes / Status");
                    updateBtn.addActionListener(e -> {
                        Window w = SwingUtilities.getWindowAncestor(panel);
                        if (w != null) w.dispose();
                        openUpdateDialog(app);
                    });
                    actionPanel.add(new JSeparator());
                    actionPanel.add(updateBtn);
                } else {
                    actionPanel.add(new JSeparator());
                    actionPanel.add(new JLabel("<html><i style='color:gray;'>Read-only Mode.</i></html>"));
                }

                panel.add(actionPanel, BorderLayout.SOUTH);

                JOptionPane.showMessageDialog(this, panel, "Treatment Details", JOptionPane.PLAIN_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    // NEW: The Dentist-only feature to update records
  private void openUpdateDialog(Appointment app) {
    JPanel editPanel = new JPanel(new BorderLayout(10, 10));

    // 1. Create the text area
    JTextArea notesArea = new JTextArea(8, 30); // 8 rows, 30 columns
    notesArea.setLineWrap(true);
    notesArea.setWrapStyleWord(true);
    notesArea.setText(app.getClinicalNotes());

    // 2. WRAP IT in a JScrollPane
    JScrollPane scrollPane = new JScrollPane(notesArea);
    
    // 3. SET A FIXED SIZE for the scrollable area
    // This prevents the dialog from growing with the text
    scrollPane.setPreferredSize(new Dimension(400, 150)); 

    String[] statuses = {"Completed", "Cancelled", "Follow-up Required"};
    JComboBox<String> statusBox = new JComboBox<>(statuses);
    statusBox.setSelectedItem(app.getStatus());

    // Layout the components
    JPanel inputs = new JPanel(new GridLayout(0, 1, 5, 5));
    inputs.add(new JLabel("Update Status:"));
    inputs.add(statusBox);
    inputs.add(new JLabel("Clinical Notes:"));
    
    editPanel.add(inputs, BorderLayout.NORTH);
    editPanel.add(scrollPane, BorderLayout.CENTER); // Add the scrollPane, NOT the notesArea directly

    int result = JOptionPane.showConfirmDialog(this, editPanel, 
                 "Update Treatment Record", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String newStatus = (String) statusBox.getSelectedItem();
                String newNotes = notesArea.getText(); // Capture the text from the UI

                // FIX: Use updateTreatmentRecord so it actually sends the notes to the DAO!
                boolean success = appService.updateTreatmentRecord(app.getAppointmentId(), newStatus, newNotes);

                if (success) {
                    // Update the local object immediately so if you click it again without refreshing, it shows up
                    app.setStatus(newStatus);
                    app.setClinicalNotes(newNotes);

                    JOptionPane.showMessageDialog(this, "Record Updated Successfully!");
                    loadHistoryData(); // Refresh the table
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating record.");
            }
        }
    }
}