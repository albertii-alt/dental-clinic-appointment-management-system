package com.dentalclinic.patient;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;

public class BookAppointmentPanel extends JPanel {

    private JComboBox<String> serviceTypeCombo;
    private JDateChooser appointmentDatePicker;
    private JComboBox<String> timeSlotCombo;
    private JButton confirmBtn;

    public BookAppointmentPanel() {
        // Match the background color of your Dashboard content area
        setBackground(new Color(236, 240, 241));
        setLayout(null);

        // --- TITLE ---
        JLabel title = new JLabel("Book New Appointment");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(new Color(44, 62, 80));
        title.setBounds(40, 30, 400, 40);
        add(title);

        // --- SERVICE SELECTION ---
        JLabel serviceLabel = new JLabel("Select Service Type:");
        serviceLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        serviceLabel.setBounds(40, 100, 200, 25);
        add(serviceLabel);

        String[] services = {"Consultation", "Oral Prophylaxis (Cleaning)", "Extraction", "Root Canal", "Dental Braces Checkup"};
        serviceTypeCombo = new JComboBox<>(services);
        serviceTypeCombo.setBounds(40, 130, 350, 35);
        add(serviceTypeCombo);

        // --- DATE SELECTION (JCalendar) ---
        JLabel dateLabel = new JLabel("Select Appointment Date:");
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        dateLabel.setBounds(40, 190, 250, 25);
        add(dateLabel);

        appointmentDatePicker = new JDateChooser();
        appointmentDatePicker.setDateFormatString("MMMM d, yyyy");
        appointmentDatePicker.setBounds(40, 220, 350, 35);
        add(appointmentDatePicker);

        // --- TIME SLOT SELECTION (AM/PM) ---
        JLabel timeLabel = new JLabel("Select Preferred Time:");
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        timeLabel.setBounds(40, 280, 200, 25);
        add(timeLabel);

        String[] timeSlots = {
            "08:00 AM", "09:00 AM", "10:00 AM", "11:00 AM",
            "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM", "05:00 PM"
        };
        timeSlotCombo = new JComboBox<>(timeSlots);
        timeSlotCombo.setBounds(40, 310, 350, 35);
        add(timeSlotCombo);

        // --- CONFIRM BUTTON ---
        confirmBtn = new JButton("Confirm Appointment Request");
        confirmBtn.setBounds(40, 390, 350, 45);
        confirmBtn.setBackground(new Color(52, 152, 219)); // Modern Blue
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Arial", Font.BOLD, 16));
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(confirmBtn);
        
        // Success Message Placeholder (Based on Use Case: Displays "Appointment Request Submitted")
        confirmBtn.addActionListener(e -> {
            if(appointmentDatePicker.getDate() == null) {
                JOptionPane.showMessageDialog(this, "Please select a date first.");
            } else {
                JOptionPane.showMessageDialog(this, "Appointment Request Submitted!\nStatus: Pending Approval");
            }
        });
    }
}