package com.dentalclinic.staff;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.dao.AppointmentDAO;

public class StaffManageSchedulePanel extends JPanel {
    private AppointmentService appService = new AppointmentService();
    private AppointmentDAO appDAO = new AppointmentDAO(); // To access the new block methods
    private String currentStaffName;
    
    private JDateChooser datePicker;
    private JPanel slotsContainer;
    private JLabel statusLabel;
    private int currentStaffId;
    private String currentRole;

    public StaffManageSchedulePanel(int staffId, String staffName, String role) {
            this.currentStaffId = staffId;
            this.currentStaffName = staffName; // INITIALIZE IT
            this.currentRole = role;
        // Inside StaffDashboard.java
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));

        // --- TOP HEADER ---
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JLabel title = new JLabel("Manage Daily Schedule");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        header.add(title);

        datePicker = new JDateChooser(new java.util.Date());
        datePicker.setPreferredSize(new Dimension(180, 30));
        // Refresh slots whenever the date changes
        datePicker.addPropertyChangeListener("date", evt -> refreshSchedule());
        header.add(new JLabel("Select Date:"));
        header.add(datePicker);
        
                // --- QUICK ACTIONS ---
        JButton blockAllBtn = new JButton("Block All Day");
        blockAllBtn.setBackground(new Color(231, 76, 60)); // Red-ish
        blockAllBtn.setForeground(Color.WHITE);

        JButton clearAllBtn = new JButton("Clear All Blocks");
        clearAllBtn.setBackground(new Color(46, 204, 113)); // Green-ish
        clearAllBtn.setForeground(Color.WHITE);

        blockAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Block all available slots for this day?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String[] allSlots = appDAO.getDynamicTimeSlots();
                    // Pass the ID and Role here
                    appDAO.blockAllDay(new java.sql.Date(datePicker.getDate().getTime()), allSlots, currentStaffId, currentRole);
                    refreshSchedule();
                } catch (SQLException ex) { ex.printStackTrace(); }
            }
        });

        // Inside the constructor of StaffManageSchedulePanel.java
        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Clear all blocks for this day?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    // Updated call to pass ID and Role
                    appDAO.unblockAllDay(new java.sql.Date(datePicker.getDate().getTime()), currentStaffId, currentRole);
                    refreshSchedule();
                    JOptionPane.showMessageDialog(this, "All blocks cleared and recorded.");
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                    JOptionPane.showMessageDialog(this, "Error clearing blocks: " + ex.getMessage());
                }
            }
        });

        header.add(blockAllBtn);
        header.add(clearAllBtn);

        statusLabel = new JLabel("Select a date to manage slots");
        statusLabel.setForeground(Color.GRAY);
        header.add(statusLabel);

        add(header, BorderLayout.NORTH);

        // --- CENTER SLOTS AREA ---
        slotsContainer = new JPanel();
        slotsContainer.setLayout(new BoxLayout(slotsContainer, BoxLayout.Y_AXIS));
        slotsContainer.setBackground(new Color(236, 240, 241));

        JScrollPane scrollPane = new JScrollPane(slotsContainer);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Initial Load
        refreshSchedule();
    }

    private void refreshSchedule() {
        if (datePicker.getDate() == null) return;
        
        slotsContainer.removeAll();
        java.sql.Date selectedDate = new java.sql.Date(datePicker.getDate().getTime());
        
        try {
            // 1. Get ALL slots from clinic_hours (Master List)
            String[] allSlots = appDAO.getDynamicTimeSlots();
            
            // 2. Get Occupied slots (Booked by patients)
            List<String> occupied = appDAO.getOccupiedSlots(selectedDate);
            Set<String> occupiedSet = new HashSet<>(occupied);

            // 3. Get Blocked slots (Manually locked by staff)
            List<String> blocked = appDAO.getBlockedSlotsByDate(selectedDate);
            Set<String> blockedSet = new HashSet<>(blocked);

            for (String slot : allSlots) {
                slotsContainer.add(createSlotRow(slot, selectedDate, occupiedSet.contains(slot), blockedSet.contains(slot)));
                slotsContainer.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            
            statusLabel.setText("Viewing schedule for: " + selectedDate.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading schedule: " + e.getMessage());
        }

        slotsContainer.revalidate();
        slotsContainer.repaint();
    }

    private JPanel createSlotRow(String slot, java.sql.Date date, boolean isOccupied, boolean isBlocked) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(800, 50));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel timeLbl = new JLabel(slot);
        timeLbl.setFont(new Font("Arial", Font.BOLD, 14));
        row.add(timeLbl, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(Color.WHITE);

        if (isOccupied) {
            // If already booked by a patient, staff can't "block" it
            JLabel bookedLbl = new JLabel("BOOKED BY PATIENT");
            bookedLbl.setForeground(new Color(231, 76, 60)); // Red
            bookedLbl.setFont(new Font("Arial", Font.ITALIC, 12));
            actionPanel.add(bookedLbl);
        } else {
            // Toggle Button for Blocking/Unblocking
            JButton toggleBtn = new JButton(isBlocked ? "Unblock Slot" : "Block Slot");
            toggleBtn.setFocusPainted(false);
            
            if (isBlocked) {
                toggleBtn.setBackground(new Color(52, 152, 219)); // Blue for Unblock
                toggleBtn.setForeground(Color.WHITE);
                toggleBtn.addActionListener(e -> handleUnblock(date, slot));
            } else {
                toggleBtn.setBackground(new Color(149, 165, 166)); // Gray for Block
                toggleBtn.setForeground(Color.WHITE);
                toggleBtn.addActionListener(e -> handleBlock(date, slot));
            }
            actionPanel.add(toggleBtn);
        }

        row.add(actionPanel, BorderLayout.EAST);
        return row;
    }
    
    private void handleBlock(java.sql.Date date, String slot) {
        try {
            if (appDAO.blockSlot(date, slot, "Staff Manual Block", currentStaffId, currentRole)) {
                refreshSchedule();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleUnblock(java.sql.Date date, String slot) {
        try {
            if (appDAO.unblockSlot(date, slot, currentStaffId, currentRole)) {
                refreshSchedule();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}