package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class StaffManageSchedulePanel extends JPanel {
    private final AppointmentController appointmentController = new AppointmentController();
    private String currentStaffName;
    private int currentStaffId;
    private String currentRole;

    private JDateChooser datePicker;
    private JPanel slotsContainer;
    private JLabel statusLabel;

    // THEME SYNC (Matching StaffBookAppointmentPanel)
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color DANGER = new Color(231, 76, 60);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public StaffManageSchedulePanel(int staffId, String staffName, String role) {
        this.currentStaffId = staffId;
        this.currentStaffName = staffName;
        this.currentRole = role;

        setLayout(new BorderLayout());
        setBackground(BG);

        // --- TOP HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(15, 25, 15, 25)
        ));

        // Title Section
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setBackground(CARD);
        JLabel title = new JLabel("Manage Daily Schedule");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TEXT);
        
        statusLabel = new JLabel("Select a date to manage slots");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.GRAY);
        
        titlePanel.add(title);
        titlePanel.add(statusLabel);
        header.add(titlePanel, BorderLayout.WEST);

        // Controls Section
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        controls.setBackground(CARD);

        datePicker = new JDateChooser(new java.util.Date());
        datePicker.setPreferredSize(new Dimension(180, 35));
        datePicker.setDateFormatString("MMMM d, yyyy");
        datePicker.addPropertyChangeListener("date", evt -> refreshSchedule());
        
        JButton blockAllBtn = new JButton("Block All Day");
        styleButton(blockAllBtn, DANGER);
        
        JButton clearAllBtn = new JButton("Clear All Blocks");
        styleButton(clearAllBtn, SUCCESS);

        controls.add(new JLabel("Date:"));
        controls.add(datePicker);
        controls.add(blockAllBtn);
        controls.add(clearAllBtn);
        header.add(controls, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // --- CENTER SLOTS AREA ---
        slotsContainer = new JPanel();
        slotsContainer.setLayout(new BoxLayout(slotsContainer, BoxLayout.Y_AXIS));
        slotsContainer.setBackground(BG);
        slotsContainer.setBorder(new EmptyBorder(20, 40, 20, 40));

        JScrollPane scrollPane = new JScrollPane(slotsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG);
        add(scrollPane, BorderLayout.CENTER);

        // --- ACTION LISTENERS ---
        blockAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Block all available slots for this day?", "Confirm Block", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    String[] allSlots = appointmentController.getTimeSlots();
                    appointmentController.blockAllDay(datePicker.getDate(), allSlots, currentStaffId, currentRole);
                    refreshSchedule();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        clearAllBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Clear all manual blocks for this day?", "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    appointmentController.unblockAllDay(datePicker.getDate(), currentStaffId, currentRole);
                    refreshSchedule();
                    JOptionPane.showMessageDialog(this, "All blocks cleared.");
                } catch (Exception ex) { 
                    ex.printStackTrace(); 
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                }
            }
        });

        refreshSchedule();
    }

    private void refreshSchedule() {
        if (datePicker.getDate() == null) return;
        
        slotsContainer.removeAll();
        java.util.Date selectedDate = datePicker.getDate();
        
        try {
            String[] allSlots = appointmentController.getTimeSlots();
            List<String> occupied = appointmentController.getOccupiedSlots(selectedDate);
            Set<String> occupiedSet = new HashSet<>(occupied);
            List<String> blocked = appointmentController.getBlockedSlotsByDate(selectedDate);
            Set<String> blockedSet = new HashSet<>(blocked);

            for (String slot : allSlots) {
                slotsContainer.add(createSlotRow(slot, selectedDate, occupiedSet.contains(slot), blockedSet.contains(slot)));
                slotsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
            }
            
            statusLabel.setText("Currently managing: " + selectedDate.toString());

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading schedule: " + e.getMessage());
        }

        slotsContainer.revalidate();
        slotsContainer.repaint();
    }

    private JPanel createSlotRow(String slot, java.util.Date date, boolean isOccupied, boolean isBlocked) {
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(1000, 60));
        row.setBackground(CARD);
        row.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(10, 20, 10, 20)
        ));

        // Time Label
        JLabel timeLbl = new JLabel(slot);
        timeLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        timeLbl.setForeground(TEXT);
        row.add(timeLbl, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(CARD);

        if (isOccupied) {
            JLabel bookedLbl = new JLabel("OCCUPIED BY APPOINTMENT");
            bookedLbl.setForeground(DANGER);
            bookedLbl.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 12));
            actionPanel.add(bookedLbl);
        } else {
            JButton toggleBtn = new JButton(isBlocked ? "Unblock Slot" : "Block Slot");
            styleButton(toggleBtn, isBlocked ? PRIMARY : new Color(160, 170, 180));
            
            if (isBlocked) {
                toggleBtn.addActionListener(e -> handleUnblock(date, slot));
            } else {
                toggleBtn.addActionListener(e -> handleBlock(date, slot));
            }
            actionPanel.add(toggleBtn);
        }

        row.add(actionPanel, BorderLayout.EAST);
        return row;
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
    }
    
    private void handleBlock(java.util.Date date, String slot) {
        try {
            if (appointmentController.blockSlot(date, slot, "Staff Manual Block", currentStaffId, currentRole)) {
                refreshSchedule();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleUnblock(java.util.Date date, String slot) {
        try {
            if (appointmentController.unblockSlot(date, slot, currentStaffId, currentRole)) {
                refreshSchedule();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
