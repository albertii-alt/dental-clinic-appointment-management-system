package com.dentalclinic.patient;

import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.IDateEvaluator;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        setBackground(new Color(245, 247, 250));
        setLayout(null);

        int startX = 225; 

        // --- HEADER ---
        JLabel title = new JLabel("Book New Appointment");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(41, 128, 185));
        title.setBounds(startX, 20, 450, 40);
        add(title);

        JLabel infoTitle = new JLabel("Verify your information for this visit:");
        infoTitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoTitle.setForeground(Color.GRAY);
        infoTitle.setBounds(startX, 60, 400, 20);
        add(infoTitle);

        // --- PATIENT INFO FORM ---
        createLabel("First Name", startX, 95);
        fNameField = createField(fName, startX, 120, 125);
        
        createLabel("Middle Name", startX + 135, 95);
        mNameField = createField(mName, startX + 135, 120, 125);
        
        createLabel("Last Name", startX + 270, 95);
        lNameField = createField(lName, startX + 270, 120, 130);

        createLabel("Saved Birthdate", startX, 160);
        JTextField dobDisplay = createField(dob, startX, 185, 150);
        dobDisplay.setEditable(false);
        dobDisplay.setBackground(new Color(230, 230, 230));

        createLabel("Age", startX + 160, 160);
        ageField = createField(age, startX + 160, 185, 60);

        createLabel("Current Contact No.", startX + 230, 160);
        contactField = createField(contact, startX + 230, 185, 170);

        createLabel("Current Full Address", startX, 225);
        addressField = createField(address, startX, 250, 400);

        // SEPARATOR
        JSeparator sep = new JSeparator();
        sep.setBounds(startX, 300, 400, 2);
        add(sep);

        // --- BOOKING DETAILS ---
        try {
            createLabel("1. Choose Service", startX, 315, 14);
            serviceTypeCombo = new JComboBox<>(appService.getServiceList());
            serviceTypeCombo.setBounds(startX, 340, 400, 35);
            add(serviceTypeCombo);

            createLabel("2. Pick a Date", startX, 385, 14);
            appointmentDatePicker = new JDateChooser();
            appointmentDatePicker.setDateFormatString("MMMM d, yyyy");
            appointmentDatePicker.setBounds(startX, 410, 400, 35);
            
            // Set min date based on lead time
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, appService.getBookingLeadTime());
            appointmentDatePicker.setMinSelectableDate(cal.getTime());
            
            applyCalendarFilter(appService.getClosedDays());
            add(appointmentDatePicker);

            createLabel("3. Select Time Slot", startX, 455, 14);
            timeSlotCombo = new JComboBox<>(new String[]{"-- Select Date First --"});
            timeSlotCombo.setBounds(startX, 480, 400, 35);
            add(timeSlotCombo);

            // Listener to update slots when date changes
            appointmentDatePicker.addPropertyChangeListener("date", evt -> {
                if ("date".equals(evt.getPropertyName())) refreshTimeSlots();
            });

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // CONFIRM BUTTON
        confirmBtn = new JButton("Confirm Appointment Request");
        confirmBtn.setBounds(startX, 540, 400, 50);
        confirmBtn.setBackground(new Color(46, 204, 113));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBtn.addActionListener(e -> handleBooking());
        add(confirmBtn);
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
                for (String slot : available) timeSlotCombo.addItem(slot);
                confirmBtn.setEnabled(true);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleBooking() {
        if (appointmentDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please select a date.");
            return;
        }

        String selectedTime = (String) timeSlotCombo.getSelectedItem();
        if (selectedTime == null || selectedTime.equals("Fully Booked") || selectedTime.contains("Select Date")) {
            JOptionPane.showMessageDialog(this, "Please select a valid time slot.");
            return;
        }

        try {
            if (!appService.canPatientBook(patientID)) {
                JOptionPane.showMessageDialog(this, "You currently have a request pending approval.");
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

            int generatedID = appService.createAppointment(newApp); 

            if (generatedID != -1) {
                showBookingSummary(newApp, generatedID); 
                confirmBtn.setEnabled(false);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // --- HELPER UI METHODS ---
    private void createLabel(String text, int x, int y) { createLabel(text, x, y, 12); }

    private void createLabel(String text, int x, int y, int fontSize) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        lbl.setBounds(x, y, 300, 20);
        add(lbl);
    }

    private JTextField createField(String text, int x, int y, int w) {
        JTextField field = new JTextField(text);
        field.setBounds(x, y, w, 32);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        add(field);
        return field;
    }

    private void applyCalendarFilter(List<String> closedDayNames) {
        Set<Integer> closedDays = new HashSet<>();
        for (String day : closedDayNames) {
            switch(day.toLowerCase()) {
                case "sunday": closedDays.add(Calendar.SUNDAY); break;
                case "monday": closedDays.add(Calendar.MONDAY); break;
                case "tuesday": closedDays.add(Calendar.TUESDAY); break;
                case "wednesday": closedDays.add(Calendar.WEDNESDAY); break;
                case "thursday": closedDays.add(Calendar.THURSDAY); break;
                case "friday": closedDays.add(Calendar.FRIDAY); break;
                case "saturday": closedDays.add(Calendar.SATURDAY); break;
            }
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
            @Override public Color getInvalidBackroundColor() { return new Color(245, 245, 245); }
            @Override public String getInvalidTooltip() { return "Closed"; }
        });
    }

    private void showBookingSummary(Appointment app, int refID) {
        JDialog receipt = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Success", true);
        receipt.setLayout(new BorderLayout());
        receipt.setSize(400, 500);
        receipt.setLocationRelativeTo(this);

        JPanel mainP = new JPanel();
        mainP.setLayout(new BoxLayout(mainP, BoxLayout.Y_AXIS));
        mainP.setBorder(new EmptyBorder(25, 35, 25, 35));
        mainP.setBackground(Color.WHITE);

        String receiptText = "<html><div style='text-align: center; font-family: Segoe UI;'>" +
            "<h1 style='color: #27ae60; margin-bottom: 0;'>Booking Sent!</h1>" +
            "<p style='color: #7f8c8d;'>Your request is now in our queue.</p>" +
            "<div style='background-color: #f9f9f9; padding: 15px; border-radius: 10px;'>" +
            "<p style='font-size: 16px;'><b>Ref ID: #" + refID + "</b></p>" +
            "<table style='width: 100%; margin-top: 10px;'>" +
            "<tr><td style='color: #95a5a6;'>Service:</td><td>" + app.getServiceType() + "</td></tr>" +
            "<tr><td style='color: #95a5a6;'>Date:</td><td>" + app.getAppointmentDate() + "</td></tr>" +
            "<tr><td style='color: #95a5a6;'>Time:</td><td>" + app.getAppointmentTime() + "</td></tr>" +
            "<tr><td style='color: #95a5a6;'>Status:</td><td style='color: #e67e22;'><b>PENDING</b></td></tr>" +
            "</table></div>" +
            "<p style='font-size: 11px; color: #bdc3c7; margin-top: 20px;'>" +
            "Admin approval is required. You will see an update in your history shortly.</p>" +
            "</div></html>";

        JLabel contentLbl = new JLabel(receiptText);
        JButton closeBtn = new JButton("Got it!");
        closeBtn.setPreferredSize(new Dimension(200, 40));
        closeBtn.setBackground(new Color(41, 128, 185));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.addActionListener(e -> receipt.dispose());

        mainP.add(contentLbl);
        mainP.add(Box.createVerticalStrut(25));
        mainP.add(closeBtn);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        receipt.add(mainP);
        receipt.setVisible(true);
    }
}