package com.dentalclinic.patient;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.toedter.calendar.IDateEvaluator;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class BookAppointmentPanel extends JPanel {

    private JComboBox<String> serviceTypeCombo;
    private JDateChooser appointmentDatePicker;
    private JComboBox<String> timeSlotCombo;
    private JButton confirmBtn;
    private JTextField fNameField, mNameField, lNameField, dobField, ageField, addressField, contactField;
    private int patientID;

    // Added 'String age' to the constructor parameters
    public BookAppointmentPanel(int pID, String fName, String mName, String lName, String dob, String age, String address, String contact) {
        setBackground(new Color(236, 240, 241));
        setLayout(null);
        this.patientID = pID;

        // Center alignment variable
        int startX = 225; 

        // --- TITLE ---
        JLabel title = new JLabel("Book New Appointment");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        title.setBounds(startX, 20, 400, 40);
        add(title);
        
        // --- PATIENT INFORMATION SECTION ---
        JLabel infoTitle = new JLabel("Patient Information (Verify/Edit for this visit)");
        infoTitle.setFont(new Font("Arial", Font.ITALIC, 14));
        infoTitle.setBounds(startX, 65, 400, 20);
        add(infoTitle);

        // Labels for Row 1
        createLabel("First Name:", startX, 90);
        createLabel("Middle Name:", startX + 135, 90);
        createLabel("Last Name:", startX + 270, 90);

        fNameField = createField(fName, startX, 115, 125);
        mNameField = createField(mName, startX + 135, 115, 125);
        lNameField = createField(lName, startX + 270, 115, 130);

        // Labels for Row 2
        createLabel("Birthdate:", startX, 150);
        createLabel("Age:", startX + 160, 150);
        createLabel("Contact No:", startX + 230, 150);

        dobField = createField(dob, startX, 175, 150);
        ageField = createField(age, startX + 160, 175, 60);
        contactField = createField(contact, startX + 230, 175, 170);

        // Labels for Row 3
        createLabel("Full Address:", startX, 210);
        addressField = createField(address, startX, 235, 400);

        // --- SEPARATOR LINE ---
        JSeparator sep = new JSeparator();
        sep.setBounds(startX, 280, 400, 2);
        add(sep);

        // --- BOOKING SECTION ---
        JLabel serviceLabel = new JLabel("Select Service Type:");
        serviceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        serviceLabel.setBounds(startX, 295, 200, 25);
        add(serviceLabel);

        // Replace the old String[] services and new JComboBox(services) with:
        serviceTypeCombo = new JComboBox<>(getDynamicServices());
        serviceTypeCombo.setBounds(startX, 320, 400, 35);
        add(serviceTypeCombo);

        JLabel dateLabel = new JLabel("Select Appointment Date:");
        dateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dateLabel.setBounds(startX, 365, 250, 25);
        add(dateLabel);

        appointmentDatePicker = new JDateChooser();
        appointmentDatePicker.setDateFormatString("MMMM d, yyyy");
        appointmentDatePicker.setBounds(startX, 390, 400, 35);
        add(appointmentDatePicker);
        applyCalendarFilter();
        // Inside the constructor
        Calendar cal = Calendar.getInstance();
        int leadDays = getLeadTimeFromDB(); // Fetches 3 (or whatever Admin set)
        cal.add(Calendar.DAY_OF_MONTH, leadDays);
        appointmentDatePicker.setMinSelectableDate(cal.getTime());

        JLabel timeLabel = new JLabel("Select Preferred Time:");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        timeLabel.setBounds(startX, 435, 200, 25);
        add(timeLabel);

        // Replace the old String[] timeSlots and new JComboBox(timeSlots) with:
        timeSlotCombo = new JComboBox<>(getDynamicTimeSlots());
        timeSlotCombo.setBounds(startX, 460, 400, 35);
        add(timeSlotCombo);
        
        // --- CONFIRM BUTTON ---
        confirmBtn = new JButton("Confirm Appointment Request");
        confirmBtn.setBounds(startX, 520, 400, 45);
        confirmBtn.setBackground(new Color(52, 152, 219));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 16));
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(confirmBtn);
        
        confirmBtn.addActionListener(e -> {
            if (appointmentDatePicker.getDate() == null) {
                JOptionPane.showMessageDialog(this, "Please select a date first.");
                return;
            }

            // Get values from UI
            String selectedService = (String) serviceTypeCombo.getSelectedItem();
            String selectedTime = (String) timeSlotCombo.getSelectedItem();

            // Format Date for SQL (YYYY-MM-DD)
            java.util.Date date = appointmentDatePicker.getDate();
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());

            // SQL Query matching the table we created earlier
            String insertQuery = "INSERT INTO appointments (patient_id, service_type, appointment_date, appointment_time, age_at_visit, contact_at_visit, status) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, 'Pending')";

            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dental_clinic_db", "root", "");
                 PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

                pstmt.setInt(1, patientID);
                pstmt.setString(2, selectedService);
                pstmt.setDate(3, sqlDate);
                pstmt.setString(4, selectedTime);
                pstmt.setInt(5, Integer.parseInt(ageField.getText())); // Captures edited age
                pstmt.setString(6, contactField.getText());           // Captures edited contact

                int rowsInserted = pstmt.executeUpdate();
                if (rowsInserted > 0) {
                    JOptionPane.showMessageDialog(this, "Appointment Request Submitted Successfully!\nStatus: Pending Approval");

                    // Optional: Disable button to prevent double booking
                    confirmBtn.setEnabled(false);
                    confirmBtn.setText("Request Sent");
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        });
    }

    // Helper method to create labels quickly
    private void createLabel(String text, int x, int y) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));
        lbl.setBounds(x, y, 120, 20);
        add(lbl);
    }

    // Helper method to create text fields quickly
    private JTextField createField(String text, int x, int y, int w) {
        JTextField field = new JTextField(text);
        field.setBounds(x, y, w, 30);
        add(field);
        return field;
    }

        // Method to fetch Services from Database
    private String[] getDynamicServices() {
        List<String> services = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dental_clinic_db", "root", "")) {
            String query = "SELECT service_name FROM services"; // Add 'WHERE is_active = 1' if you used that column
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                services.add(rs.getString("service_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[]{"Consultation", "Cleaning"}; // Fallback if DB fails
        }
        return services.toArray(new String[0]);
    }

// Method to fetch Time Slots from Database
    private String[] getDynamicTimeSlots() {
        List<String> slots = new ArrayList<>();
        // Using try-with-resources to ensure the connection closes automatically
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dental_clinic_db", "root", "")) {
            // MATCHED: 'time_slot' column from your SQL
            String query = "SELECT time_slot FROM clinic_hours WHERE is_active = 1";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                slots.add(rs.getString("time_slot"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Fallback slots if the database connection fails
            return new String[]{"08:00 AM", "09:00 AM", "01:00 PM"}; 
        }
        return slots.toArray(new String[0]);
    }
    private void applyCalendarFilter() {
        Set<Integer> closedDays = new HashSet<>();

        // 1. Fetch closed days from clinic_schedule
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dental_clinic_db", "root", "")) {
            String query = "SELECT day_name FROM clinic_schedule WHERE is_open = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);

            while (rs.next()) {
                String day = rs.getString("day_name");
                if (day.equalsIgnoreCase("Sunday")) closedDays.add(Calendar.SUNDAY);
                if (day.equalsIgnoreCase("Monday")) closedDays.add(Calendar.MONDAY);
                if (day.equalsIgnoreCase("Tuesday")) closedDays.add(Calendar.TUESDAY);
                if (day.equalsIgnoreCase("Wednesday")) closedDays.add(Calendar.WEDNESDAY);
                if (day.equalsIgnoreCase("Thursday")) closedDays.add(Calendar.THURSDAY);
                if (day.equalsIgnoreCase("Friday")) closedDays.add(Calendar.FRIDAY);
                if (day.equalsIgnoreCase("Saturday")) closedDays.add(Calendar.SATURDAY);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    // 2. Apply the "Bouncer" to the DateChooser
    appointmentDatePicker.getJCalendar().getDayChooser().addDateEvaluator(new IDateEvaluator() {
        @Override
        public boolean isSpecial(java.util.Date date) { return false; }
        
        @Override
        public boolean isInvalid(java.util.Date date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            
            // If the day is in our "closedDays" set, return true (invalid/disabled)
            return closedDays.contains(dayOfWeek);
        }

        @Override public Color getSpecialForegroundColor() { return null; }
        @Override public Color getSpecialBackroundColor() { return null; }
        @Override public String getSpecialTooltip() { return "Clinic is Closed"; }
        @Override public Color getInvalidForegroundColor() { return Color.RED; }
        @Override public Color getInvalidBackroundColor() { return new Color(240, 240, 240); }
        @Override public String getInvalidTooltip() { return "Closed"; }
        });
    }
    
    private int getLeadTimeFromDB() {
        int days = 0; // Default if DB fails
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/dental_clinic_db", "root", "")) {
            String query = "SELECT setting_value FROM clinic_settings WHERE setting_name = 'min_lead_days'";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                days = rs.getInt("setting_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return days;
    }
}