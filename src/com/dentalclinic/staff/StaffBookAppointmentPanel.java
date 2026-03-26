// UI ENHANCED VERSION (LOGIC UNCHANGED)
package com.dentalclinic.staff;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.dao.PatientDAO;

public class StaffBookAppointmentPanel extends JPanel {
    private AppointmentService appService = new AppointmentService();
    private PatientDAO patientDAO = new PatientDAO();
    private JLabel stepLabel;
    private JTextField searchField;
    private JComboBox<String> patientResultsCombo;
    private List<Object[]> currentSearchResults;
    
    private JTextField fNameField, lNameField, ageField, contactField;
    private JComboBox<String> serviceTypeCombo, timeSlotCombo;
    private JDateChooser appointmentDatePicker;
    private int selectedPatientID = -1;

    // THEME
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final int SPACING = 12;

    public StaffBookAppointmentPanel() {
        // Initialize Components
        searchField = new JTextField();
        patientResultsCombo = new JComboBox<>();
        fNameField = new JTextField();
        contactField = new JTextField();
        ageField = new JTextField();
        timeSlotCombo = new JComboBox<>();
        serviceTypeCombo = new JComboBox<>();
        appointmentDatePicker = new JDateChooser();

        setLayout(new GridBagLayout());
        setBackground(BG);

        // Main Card Container
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setPreferredSize(new Dimension(500, 750));
        container.setBackground(CARD);
        container.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 220), 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));

        // TITLE
        JLabel title = new JLabel("Staff Booking Portal");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(title);
        
        JLabel subtitle = new JLabel("Create and auto-approve appointments");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(subtitle);
        
        container.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // -------- STEP 1: PATIENT SELECTION --------
        container.add(createSectionLabel("Step 1: Patient Information"));
        container.add(Box.createRigidArea(new Dimension(0, 8)));
        
        container.add(createLabelOnly("Search Patient Name:"));
        container.add(createInput(searchField));
        container.add(Box.createRigidArea(new Dimension(0, 5)));
        container.add(createCombo(patientResultsCombo));
        
        container.add(Box.createRigidArea(new Dimension(0, SPACING)));
        container.add(createFieldWithLabel("Selected Patient Name", fNameField, false));
        container.add(createFieldWithLabel("Contact Number", contactField, true));

        container.add(Box.createRigidArea(new Dimension(0, 10)));
        container.add(createDivider());
        container.add(Box.createRigidArea(new Dimension(0, 10)));

        // -------- STEP 2: SCHEDULE --------
        container.add(createSectionLabel("Step 2: Service & Schedule"));
        container.add(Box.createRigidArea(new Dimension(0, 8)));

        try {
            String[] services = appService.getServiceList();
            if (services != null) serviceTypeCombo.setModel(new DefaultComboBoxModel<>(services));
        } catch (Exception e) {}

        container.add(createLabelOnly("Select Service:"));
        container.add(createCombo(serviceTypeCombo));
        
        container.add(Box.createRigidArea(new Dimension(0, SPACING)));
        
        container.add(createLabelOnly("Appointment Date:"));
        appointmentDatePicker.setDateFormatString("MMMM d, yyyy");
        container.add(createDatePicker(appointmentDatePicker));

        container.add(Box.createRigidArea(new Dimension(0, SPACING)));
        
        container.add(createLabelOnly("Available Time Slot:"));
        container.add(createCombo(timeSlotCombo));

        // Logic for Date restrictions
        try {
            int leadTime = appService.getBookingLeadTime();
            java.util.List<String> closedDays = appService.getClosedDays();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.DAY_OF_MONTH, leadTime);
            appointmentDatePicker.setMinSelectableDate(cal.getTime());
            applyCalendarFilter(closedDays);
        } catch (SQLException e) {
            appointmentDatePicker.setMinSelectableDate(new java.util.Date());
        }

        // BUTTON
        container.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton confirmBtn = new JButton("Confirm & Approve Appointment");
        styleButton(confirmBtn, SUCCESS);
        confirmBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        container.add(confirmBtn);

        // LISTENERS
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                refreshPatientDropdown(searchField.getText());
            }
        });

        patientResultsCombo.addActionListener(e -> selectPatient());
        appointmentDatePicker.addPropertyChangeListener("date", evt -> refreshSlots());
        confirmBtn.addActionListener(e -> handleStaffBooking());

        try { refreshPatientDropdown(""); } catch (Exception e) {}

        GridBagConstraints gbc = new GridBagConstraints();
        add(container, gbc);
    }

    // ---------- UI HELPERS ----------

    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel createLabelOnly(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private JComponent createInput(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private JComponent createCombo(JComboBox<?> combo) {
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        combo.setBackground(Color.WHITE);
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        return combo;
    }

    private JComponent createDatePicker(JDateChooser picker) {
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        picker.setAlignmentX(Component.LEFT_ALIGNMENT);
        return picker;
    }

    private JPanel createFieldWithLabel(String label, JTextField field, boolean editable) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(createLabelOnly(label));
        panel.add(createInput(field));
        field.setEditable(editable);
        
        if (!editable) {
            field.setBackground(new Color(245, 245, 245));
        }

        return panel;
    }

    private JSeparator createDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 230, 230));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
    }

    // ---------- LOGIC (UNCHANGED) ----------

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

    private void refreshPatientDropdown(String query) {
        try {
            if (query.trim().isEmpty()) {
                currentSearchResults = patientDAO.getAllPatients();
            } else {
                currentSearchResults = patientDAO.searchPatientsByName(query);
            }
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (Object[] p : currentSearchResults) {
                model.addElement(p[1] + " (ID: " + p[0] + ")");
            }
            patientResultsCombo.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshSlots() {
        if (appointmentDatePicker.getDate() == null) return;
        try {
            java.util.List<String> available = appService.getAvailableSlotsForDate(appointmentDatePicker.getDate());
            timeSlotCombo.removeAllItems();
            if (available.isEmpty()) {
                timeSlotCombo.addItem("Fully Booked");
            } else {
                for (String s : available) timeSlotCombo.addItem(s);
            }
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
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void applyCalendarFilter(java.util.List<String> closedDayNames) {
        java.util.Set<Integer> closedDays = new java.util.HashSet<>();
        for (String day : closedDayNames) {
            if (day.equalsIgnoreCase("Sunday")) closedDays.add(java.util.Calendar.SUNDAY);
            else if (day.equalsIgnoreCase("Monday")) closedDays.add(java.util.Calendar.MONDAY);
            else if (day.equalsIgnoreCase("Tuesday")) closedDays.add(java.util.Calendar.TUESDAY);
            else if (day.equalsIgnoreCase("Wednesday")) closedDays.add(java.util.Calendar.WEDNESDAY);
            else if (day.equalsIgnoreCase("Thursday")) closedDays.add(java.util.Calendar.THURSDAY);
            else if (day.equalsIgnoreCase("Friday")) closedDays.add(java.util.Calendar.FRIDAY);
            else if (day.equalsIgnoreCase("Saturday")) closedDays.add(java.util.Calendar.SATURDAY);
        }

        appointmentDatePicker.getJCalendar().getDayChooser().addDateEvaluator(new com.toedter.calendar.IDateEvaluator() {
            @Override public boolean isInvalid(java.util.Date date) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(date);
                return closedDays.contains(cal.get(java.util.Calendar.DAY_OF_WEEK));
            }
            @Override public boolean isSpecial(java.util.Date date) { return false; }
            @Override public Color getSpecialForegroundColor() { return null; }
            @Override public Color getSpecialBackroundColor() { return null; }
            @Override public String getSpecialTooltip() { return null; }
            @Override public Color getInvalidForegroundColor() { return Color.RED; }
            @Override public Color getInvalidBackroundColor() { return new Color(240, 240, 240); }
            @Override public String getInvalidTooltip() { return "Clinic Closed"; }
        });
    }

    public void cleanup() {
        if (appointmentDatePicker != null) {
            appointmentDatePicker.getJCalendar().setVisible(false);
        }
    }
}