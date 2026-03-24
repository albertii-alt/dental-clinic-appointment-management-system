package com.dentalclinic.staff;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.dao.PatientDAO;

public class StaffBookAppointmentPanel extends JPanel {
    private AppointmentService appService = new AppointmentService();
    private PatientDAO patientDAO = new PatientDAO();
    
    private JTextField searchField;
    private JComboBox<String> patientResultsCombo;
    private List<Object[]> currentSearchResults;
    
    private JTextField fNameField, lNameField, ageField, contactField;
    private JComboBox<String> serviceTypeCombo, timeSlotCombo;
    private JDateChooser appointmentDatePicker;
    private int selectedPatientID = -1;

    public StaffBookAppointmentPanel() {
        // 1. Initialize ALL UI components FIRST (Safety First)
        searchField = new JTextField();
        patientResultsCombo = new JComboBox<>();
        fNameField = new JTextField();
        contactField = new JTextField();
        ageField = new JTextField(); // Critical fix
        timeSlotCombo = new JComboBox<>();
        serviceTypeCombo = new JComboBox<>(); // Initialize empty so it's not null
        appointmentDatePicker = new JDateChooser();

        setLayout(new GridBagLayout());
        setBackground(new Color(236, 240, 241));

        // 2. Build the White Container
        JPanel container = new JPanel(null);
        container.setPreferredSize(new Dimension(500, 650));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        // --- Add Components to Container ---
        JLabel searchLabel = new JLabel("Step 1: Search/Select Patient");
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        searchLabel.setBounds(30, 20, 200, 25);
        container.add(searchLabel);

        searchField.setBounds(30, 50, 300, 30);
        container.add(searchField);

        patientResultsCombo.setBounds(30, 90, 400, 30);
        container.add(patientResultsCombo);

        addLabel(container, "Patient Name:", 140);
        fNameField.setBounds(30, 165, 400, 30);
        fNameField.setEditable(false);
        container.add(fNameField);

        addLabel(container, "Contact No:", 205);
        contactField.setBounds(30, 230, 400, 30);
        container.add(contactField);

        addLabel(container, "Step 2: Select Service & Date", 280);

        // Safety load for services
        try {
            String[] services = appService.getServiceList();
            if(services != null) {
                serviceTypeCombo.setModel(new DefaultComboBoxModel<>(services));
            }
        } catch (Exception e) { System.err.println("Service load failed"); }

        serviceTypeCombo.setBounds(30, 310, 400, 35);
        container.add(serviceTypeCombo);

        appointmentDatePicker.setBounds(30, 360, 400, 35);
        appointmentDatePicker.setMinSelectableDate(new java.util.Date());
        container.add(appointmentDatePicker);

        timeSlotCombo.setBounds(30, 410, 400, 35);
        container.add(timeSlotCombo);

        JButton confirmBtn = new JButton("Confirm & Approve Appointment");
        confirmBtn.setBounds(30, 480, 400, 45);
        confirmBtn.setBackground(new Color(46, 204, 113));
        confirmBtn.setForeground(Color.WHITE);
        container.add(confirmBtn);

        // 3. Setup Logic/Listeners
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                refreshPatientDropdown(searchField.getText());
            }
        });

        patientResultsCombo.addActionListener(e -> selectPatient());
        appointmentDatePicker.addPropertyChangeListener("date", evt -> refreshSlots());
        confirmBtn.addActionListener(e -> handleStaffBooking());

        // 4. Initial Data Load (Wrapped in Try-Catch to prevent Panel-Kill)
        try {
            refreshPatientDropdown(""); 
        } catch (Exception e) {
            System.err.println("Initial patient load failed");
        }

        // 5. THE FIX: Center the container using GridBagConstraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER; 
        add(container, gbc);
    }

    private void selectPatient() {
        int idx = patientResultsCombo.getSelectedIndex();
        if (idx >= 0 && currentSearchResults != null && idx < currentSearchResults.size()) {
            Object[] p = currentSearchResults.get(idx);
            selectedPatientID = (int) p[0];
            fNameField.setText((String) p[1]);
            contactField.setText((String) p[4]);

            java.sql.Date dob = (java.sql.Date) p[2];
            if (dob != null) {
                int age = java.time.Period.between(dob.toLocalDate(), java.time.LocalDate.now()).getYears();
                ageField.setText(String.valueOf(age));
            }
        }
    }

    // New method to handle both "Show All" and "Search"
    private void refreshPatientDropdown(String query) {
        try {
            if (query.trim().isEmpty()) {
                // You'll need to add this method to PatientDAO as we discussed!
                currentSearchResults = patientDAO.getAllPatients(); 
            } else {
                currentSearchResults = patientDAO.searchPatientsByName(query);
            }

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (Object[] p : currentSearchResults) {
                model.addElement(p[1] + " (ID: " + p[0] + ")");
            }
            patientResultsCombo.setModel(model);

            // Show popup if filtering
            if (!query.isEmpty() && model.getSize() > 0) {
                patientResultsCombo.setPopupVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        try {
            currentSearchResults = patientDAO.searchPatientsByName(query);

            // This makes sure we don't duplicate names in the list
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (Object[] p : currentSearchResults) {
                model.addElement(p[1] + " (ID: " + p[0] + ")");
            }
            patientResultsCombo.setModel(model);

            // If results are found, show the dropdown automatically
            if (model.getSize() > 0) {
                patientResultsCombo.setPopupVisible(true);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Search Error: " + e.getMessage());
        }
    }

    private void refreshSlots() {
        if (appointmentDatePicker.getDate() == null) return;
        try {
            List<String> available = appService.getAvailableSlotsForDate(appointmentDatePicker.getDate());
            timeSlotCombo.removeAllItems();
            for (String s : available) timeSlotCombo.addItem(s);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleStaffBooking() {
        if (selectedPatientID == -1) {
            JOptionPane.showMessageDialog(this, "Please select a patient first.");
            return;
        }
        if (appointmentDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please select a date.");
            return;
        }

        try {
            // Safe parsing of age
            int ageValue = 0;
            if (!ageField.getText().isEmpty()) {
                ageValue = Integer.parseInt(ageField.getText());
            }

            Appointment app = new Appointment(
                selectedPatientID,
                (String) serviceTypeCombo.getSelectedItem(),
                new java.sql.Date(appointmentDatePicker.getDate().getTime()),
                (String) timeSlotCombo.getSelectedItem(),
                ageValue,
                contactField.getText(),
                "Approved"
            );

            int result = appService.createAppointment(app);
            if (result != -1) {
                JOptionPane.showMessageDialog(this, "Appointment Booked and Approved!");
                // Optional: Clear fields here if you want to book another one
            }
        } catch (Exception ex) { 
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); 
        }
    }

    private void addLabel(JPanel p, String text, int y) {
        JLabel l = new JLabel(text);
        l.setBounds(30, y, 200, 20);
        l.setFont(new Font("Arial", Font.BOLD, 12));
        p.add(l);
    }

    private JTextField createField(JPanel p, int y, int w) {
        JTextField f = new JTextField();
        f.setBounds(30, y, w, 30);
        p.add(f);
        return f;
    }
}