package com.dentalclinic.patient;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.IDateEvaluator;
import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.SQLException;

import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.model.Appointment;

public class BookAppointmentPanel extends JPanel {
    
    private AppointmentService appService = new AppointmentService();
    private JComboBox<String> serviceTypeCombo;
    private JDateChooser appointmentDatePicker;
    private JComboBox<String> timeSlotCombo;
    private JButton confirmBtn;
    private JTextField fNameField, mNameField, lNameField, ageField, addressField, contactField;
    private int patientID;

    public BookAppointmentPanel(int pID, String fName, String mName, String lName, String dob, String age, String address, String contact) {
        this.patientID = pID;
        setBackground(new Color(236, 240, 241));
        setLayout(null);

        int startX = 225; 

        // --- TITLE ---
        JLabel title = new JLabel("Book New Appointment");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        title.setBounds(startX, 20, 400, 40);
        add(title);

        // --- PATIENT INFORMATION SECTION ---
        JLabel infoTitle = new JLabel("Patient Information (Verify for this visit)");
        infoTitle.setFont(new Font("Arial", Font.ITALIC, 14));
        infoTitle.setBounds(startX, 65, 400, 20);
        add(infoTitle);

        // Row 1: Names
        createLabel("First Name:", startX, 90);
        createLabel("Middle Name:", startX + 135, 90);
        createLabel("Last Name:", startX + 270, 90);
        fNameField = createField(fName, startX, 115, 125);
        mNameField = createField(mName, startX + 135, 115, 125);
        lNameField = createField(lName, startX + 270, 115, 130);

        // Row 2: Age and Contact
        createLabel("Birthdate (Saved):", startX, 150);
        createLabel("Age:", startX + 160, 150);
        createLabel("Contact No:", startX + 230, 150);
        JTextField dobDisplay = createField(dob, startX, 175, 150); // Just for display
        dobDisplay.setEditable(false);
        ageField = createField(age, startX + 160, 175, 60);
        contactField = createField(contact, startX + 230, 175, 170);

        // Row 3: Address
        createLabel("Full Address:", startX, 210);
        addressField = createField(address, startX, 235, 400);

        // --- SEPARATOR ---
        JSeparator sep = new JSeparator();
        sep.setBounds(startX, 280, 400, 2);
        add(sep);

        // --- BOOKING SECTION ---
        createLabel("Select Service Type:", startX, 295, 14);
        try {
            serviceTypeCombo = new JComboBox<>(appService.getServiceList());
            timeSlotCombo = new JComboBox<>(appService.getTimeSlots());
            
            List<String> closedDays = appService.getClosedDays();
            int leadTime = appService.getBookingLeadTime();

            serviceTypeCombo.setBounds(startX, 320, 400, 35);
            add(serviceTypeCombo);

            createLabel("Select Appointment Date:", startX, 365, 14);
            appointmentDatePicker = new JDateChooser();
            appointmentDatePicker.setDateFormatString("MMMM d, yyyy");
            appointmentDatePicker.setBounds(startX, 390, 400, 35);
            
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, leadTime);
            appointmentDatePicker.setMinSelectableDate(cal.getTime());
            
            applyCalendarFilter(closedDays);
            add(appointmentDatePicker);

            createLabel("Select Preferred Time:", startX, 435, 14);
            timeSlotCombo.setBounds(startX, 460, 400, 35);
            add(timeSlotCombo);
            appointmentDatePicker.addPropertyChangeListener("date", evt -> {
                if ("date".equals(evt.getPropertyName())) {
                    refreshTimeSlots();
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            // Fallbacks in case of DB error
            serviceTypeCombo = new JComboBox<>(new String[]{"Consultation", "Cleaning"});
            timeSlotCombo = new JComboBox<>(new String[]{"08:00 AM", "01:00 PM"});
        }

        // --- CONFIRM BUTTON ---
        confirmBtn = new JButton("Confirm Appointment Request");
        confirmBtn.setBounds(startX, 520, 400, 45);
        confirmBtn.setBackground(new Color(52, 152, 219));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 16));
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(confirmBtn);

        confirmBtn.addActionListener(e -> handleBooking());
    }
    private void handleBooking() {
        if (appointmentDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please select a date first.");
            return;
        }

        String selectedTime = (String) timeSlotCombo.getSelectedItem();
        if (selectedTime == null || selectedTime.equals("Fully Booked")) {
            JOptionPane.showMessageDialog(this, "Please select a valid time slot.");
            return;
        }

        try {
            if (!appService.canPatientBook(patientID)) {
                JOptionPane.showMessageDialog(this, "You already have a pending appointment request.");
                return;
            }

            Appointment newApp = new Appointment(
                patientID,
                (String) serviceTypeCombo.getSelectedItem(),
                new java.sql.Date(appointmentDatePicker.getDate().getTime()),
                selectedTime,
                Integer.parseInt(ageField.getText()),
                contactField.getText(),
                "Pending"
            );

            // --- UPDATED EXECUTION ---
            int generatedID = appService.createAppointment(newApp); 

            if (generatedID != -1) {
                showBookingSummary(newApp, generatedID); 
                confirmBtn.setEnabled(false);

                // Quality Service: Logic to auto-switch the user to the "View History" tab
                // Would you like the code for this navigation too?
            }

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid age.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // --- HELPER METHODS ---
    private void createLabel(String text, int x, int y) {
        createLabel(text, x, y, 12);
    }

    private void createLabel(String text, int x, int y, int fontSize) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, fontSize));
        lbl.setBounds(x, y, 300, 20);
        add(lbl);
    }

    private JTextField createField(String text, int x, int y, int w) {
        JTextField field = new JTextField(text);
        field.setBounds(x, y, w, 30);
        add(field);
        return field;
    }

    private void applyCalendarFilter(List<String> closedDayNames) {
        Set<Integer> closedDays = new HashSet<>();
        for (String day : closedDayNames) {
            if (day.equalsIgnoreCase("Sunday")) closedDays.add(Calendar.SUNDAY);
            if (day.equalsIgnoreCase("Monday")) closedDays.add(Calendar.MONDAY);
            if (day.equalsIgnoreCase("Tuesday")) closedDays.add(Calendar.TUESDAY);
            if (day.equalsIgnoreCase("Wednesday")) closedDays.add(Calendar.WEDNESDAY);
            if (day.equalsIgnoreCase("Thursday")) closedDays.add(Calendar.THURSDAY);
            if (day.equalsIgnoreCase("Friday")) closedDays.add(Calendar.FRIDAY);
            if (day.equalsIgnoreCase("Saturday")) closedDays.add(Calendar.SATURDAY);
        }

        appointmentDatePicker.getJCalendar().getDayChooser().addDateEvaluator(new IDateEvaluator() {
            @Override public boolean isInvalid(java.util.Date date) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return closedDays.contains(cal.get(Calendar.DAY_OF_WEEK));
            }
            @Override public boolean isSpecial(java.util.Date date) { return false; }
            @Override public Color getSpecialForegroundColor() { return null; }
            @Override public Color getSpecialBackroundColor() { return null; }
            @Override public String getSpecialTooltip() { return "Clinic Closed"; }
            @Override public Color getInvalidForegroundColor() { return Color.RED; }
            @Override public Color getInvalidBackroundColor() { return new Color(240, 240, 240); }
            @Override public String getInvalidTooltip() { return "Closed"; }
        });
    }
        private void refreshTimeSlots() {
        java.util.Date selectedDate = appointmentDatePicker.getDate();
        if (selectedDate == null) return;

        try {
            List<String> available = appService.getAvailableSlotsForDate(selectedDate);
            timeSlotCombo.removeAllItems();

            if (available.isEmpty()) {
                timeSlotCombo.addItem("Fully Booked");
                confirmBtn.setEnabled(false);
            } else {
                for (String slot : available) {
                    timeSlotCombo.addItem(slot);
                }
                confirmBtn.setEnabled(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
        
    private void showBookingSummary(Appointment app, int refID) {
        // Create a modal dialog (blocks the main window until closed)
        JDialog receipt = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Booking Confirmed", true);
        receipt.setLayout(new BorderLayout());
        receipt.setSize(380, 480);
        receipt.setLocationRelativeTo(this);

        JPanel mainP = new JPanel();
        mainP.setLayout(new BoxLayout(mainP, BoxLayout.Y_AXIS));
        mainP.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        mainP.setBackground(Color.WHITE);

        // Using HTML for easy formatting
        String receiptText = "<html><div style='text-align: center; font-family: Arial;'>" +
            "<h2 style='color: #2ecc71;'>Appointment Request Sent!</h2>" +
            "<p style='color: #7f8c8d;'>Please save your reference details below:</p>" +
            "<hr><br>" +
            "<table style='width: 100%; text-align: left;'>" +
            "<tr><td><b>Reference ID:</b></td><td># " + refID + "</td></tr>" +
            "<tr><td><b>Service:</b></td><td>" + app.getServiceType() + "</td></tr>" +
            "<tr><td><b>Date:</b></td><td>" + app.getAppointmentDate() + "</td></tr>" +
            "<tr><td><b>Time:</b></td><td>" + app.getAppointmentTime() + "</td></tr>" +
            "<tr><td><b>Status:</b></td><td><b style='color: #e67e22;'>PENDING</b></td></tr>" +
            "</table>" +
            "<br><hr>" +
            "<p style='font-size: 10px; color: #95a5a6;'>Wait for admin approval. Please arrive 15 minutes early.</p>" +
            "</div></html>";

        JLabel contentLbl = new JLabel(receiptText);
        contentLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton closeBtn = new JButton("Close and View History");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.setBackground(new Color(52, 152, 219));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.addActionListener(e -> receipt.dispose());

        mainP.add(contentLbl);
        mainP.add(Box.createRigidArea(new Dimension(0, 20)));
        mainP.add(closeBtn);

        receipt.add(mainP);
        receipt.setVisible(true);
    }
}