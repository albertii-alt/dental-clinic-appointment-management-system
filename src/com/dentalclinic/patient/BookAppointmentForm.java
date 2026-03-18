package com.dentalclinic.patient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BookAppointmentForm extends JFrame {

    private JComboBox<String> timeSlotCombo;
    private JButton confirmBtn;
    private JButton cancelBtn;

    // Simulated booked slots (for demo)
    private String[] bookedSlots = {"09:00 AM", "10:00 AM"}; 

    public BookAppointmentForm() {

        setTitle("Book Appointment");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Book Appointment");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setBounds(110, 20, 200, 25);
        panel.add(title);

        JLabel timeLabel = new JLabel("Select Time:");
        timeLabel.setBounds(50, 70, 100, 25);
        panel.add(timeLabel);

        // Simulated time slots
        String[] timeSlots = {"09:00 AM", "10:00 AM", "11:00 AM", "01:00 PM", "02:00 PM"};
        timeSlotCombo = new JComboBox<>(timeSlots);
        timeSlotCombo.setBounds(160, 70, 150, 25);
        panel.add(timeSlotCombo);

        confirmBtn = new JButton("Confirm");
        confirmBtn.setBounds(90, 130, 90, 30);
        panel.add(confirmBtn);

        cancelBtn = new JButton("Cancel");
        cancelBtn.setBounds(200, 130, 90, 30);
        panel.add(cancelBtn);

        add(panel);

        // Confirm button logic
        confirmBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String selectedTime = (String) timeSlotCombo.getSelectedItem();

                // Check if slot is already booked (simulated)
                boolean available = true;
                for(String slot : bookedSlots){
                    if(slot.equals(selectedTime)){
                        available = false;
                        break;
                    }
                }

                if(available){
                    JOptionPane.showMessageDialog(null, "Appointment Booked Successfully!");
                    // Add to bookedSlots for demo
                    // (In real system, insert into DB)
                } else {
                    JOptionPane.showMessageDialog(null, "Selected slot is not available. Please choose another time.");
                }
            }
        });

        cancelBtn.addActionListener(e -> {
            dispose(); // go back to Dashboard
        });

        setVisible(true);
    }

    public static void main(String[] args){
        new BookAppointmentForm();
    }
}