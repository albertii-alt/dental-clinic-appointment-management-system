package com.dentalclinic.admin;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.dao.ClinicConfigDAO;

public class ClinicSettingsPanel extends JPanel {
    private JSpinner leadTimeSpinner;
    private JCheckBox[] dayChecks;
    private List<JCheckBox> timeChecks = new ArrayList<>();
    private JTextField newTimeField;
    private JButton addTimeBtn;
    private JPanel timePanel;
    private JPanel servicePanel;
    private JTextField serviceNameField, serviceDescField, servicePriceField;
    
    private String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private AppointmentService appService = new AppointmentService();
    private ClinicConfigDAO configDAO = new ClinicConfigDAO();

    public ClinicSettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // --- MAIN SCROLLABLE CONTAINER ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(Color.WHITE);
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- TITLE ---
        JLabel title = new JLabel("Clinic Configuration");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(title);
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- 1. LEAD TIME ---
        mainContent.add(createSectionLabel("Booking Lead Time"));
        JPanel leadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leadPanel.setBackground(Color.WHITE);
        leadPanel.add(new JLabel("Patients must book at least "));
        leadTimeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 30, 1));
        leadPanel.add(leadTimeSpinner);
        leadPanel.add(new JLabel(" days in advance."));
        leadPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(leadPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- 2. OPERATING DAYS ---
        mainContent.add(createSectionLabel("Clinic Operating Days"));
        JPanel daysPanel = new JPanel(new GridLayout(2, 4, 5, 5));
        daysPanel.setBackground(Color.WHITE);
        dayChecks = new JCheckBox[7];
        for (int i = 0; i < days.length; i++) {
            dayChecks[i] = new JCheckBox(days[i]);
            dayChecks[i].setBackground(Color.WHITE);
            daysPanel.add(dayChecks[i]);
        }
        daysPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(daysPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- 3. AVAILABLE TIME SLOTS ---
        mainContent.add(createSectionLabel("Active Appointment Slots"));

        // This is the "Parent" panel that holds both AM and PM columns
        timePanel = new JPanel(new GridLayout(1, 2, 15, 0)); 
        timePanel.setBackground(Color.WHITE);
        timePanel.setPreferredSize(new Dimension(0, 300)); // Set the height for the entire section
        timePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainContent.add(timePanel);

        // Wrap the whole thing in a ScrollPane
        JScrollPane timeScroll = new JScrollPane(timePanel);
        timeScroll.setPreferredSize(new Dimension(0, 300)); 
        timeScroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        mainContent.add(timeScroll);


        // Put everything in a ScrollPane
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
        
        // --- ADD NEW TIME SLOT UI ---
        JPanel addTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addTimePanel.setBackground(Color.WHITE);

        newTimeField = new JTextField(8);
        newTimeField.setToolTipText("Format: HH:MM AM/PM (e.g., 08:30 AM)");
        addTimeBtn = new JButton("Add Time");
        
        addTimeBtn.addActionListener(e -> {
            String timeInput = newTimeField.getText().trim().toUpperCase();

            // Simple validation for AM/PM format
            if (!timeInput.matches("^(0[1-9]|1[0-2]):[0-5][0-9] (AM|PM)$")) {
                JOptionPane.showMessageDialog(this, "Please use format: HH:MM AM/PM (e.g. 09:30 AM)");
                return;
            }

        try {
            if (configDAO.addTimeSlot(timeInput)) {
                JOptionPane.showMessageDialog(this, "New time slot added!");
                newTimeField.setText("");
                refreshTimeSlotsUI();
            }
        } catch (Exception ex) { // Change SQLException to Exception
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
        });

        addTimePanel.add(new JLabel("New Slot:"));
        addTimePanel.add(newTimeField);
        addTimePanel.add(addTimeBtn);
        mainContent.add(addTimePanel); // Add this to your mainContent box layout
        // --- 4. MANAGE SERVICES ---
        mainContent.add(createSectionLabel("Clinic Services"));
        servicePanel = new JPanel();
        servicePanel.setBackground(Color.WHITE);

        JScrollPane serviceScroll = new JScrollPane(servicePanel);
        serviceScroll.setPreferredSize(new Dimension(0, 200));
        mainContent.add(serviceScroll);

        // Input fields for new Service
        JPanel serviceInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        serviceInputPanel.setBackground(Color.WHITE);

        serviceNameField = new JTextField(10);
        serviceDescField = new JTextField(10);
        servicePriceField = new JTextField(5);
        JButton addServiceBtn = new JButton("Add Service");

        addServiceBtn.addActionListener(e -> {
            try {
                String name = serviceNameField.getText().trim();
                String desc = serviceDescField.getText().trim();
                String priceStr = servicePriceField.getText().trim();

                // Name is the only strictly required field
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Service Name is required.");
                    return;
                }

                // If description is empty, use a default
                if (desc.isEmpty()) desc = "No description provided.";

                // If price is empty, default to 0.00
                double price = 0.0;
                if (!priceStr.isEmpty()) {
                    price = Double.parseDouble(priceStr);
                }

                if (configDAO.addService(name, desc, price)) {
                    JOptionPane.showMessageDialog(this, "Service Added!");
                    serviceNameField.setText("");
                    serviceDescField.setText("");
                    servicePriceField.setText("");
                    buildServiceList(); 
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for Price.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding service: " + ex.getMessage());
            }
        });

        serviceInputPanel.add(new JLabel("Name:"));
        serviceInputPanel.add(serviceNameField);
        serviceInputPanel.add(new JLabel("Desc:"));
        serviceInputPanel.add(serviceDescField);
        serviceInputPanel.add(new JLabel("Price:"));
        serviceInputPanel.add(servicePriceField);
        serviceInputPanel.add(addServiceBtn);

        mainContent.add(serviceInputPanel);

        // Finally, call the builder
        buildServiceList();

        // --- 4. SAVE BUTTON ---
        JButton saveBtn = new JButton("Apply All Changes");
        saveBtn.setFont(new Font("Arial", Font.BOLD, 14));
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(0, 50));
        saveBtn.addActionListener(e -> saveSettings());
        add(saveBtn, BorderLayout.SOUTH);

        loadCurrentSettings();
    }

    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(new Color(44, 62, 80));
        return lbl;
    }

    private void loadCurrentSettings() {
        try {
            // 1. Load Lead Time
            leadTimeSpinner.setValue(appService.getBookingLeadTime());

            // 2. Load Days
            List<String> closedDays = appService.getClosedDays();
            for (int i = 0; i < days.length; i++) {
                dayChecks[i].setSelected(!closedDays.contains(days[i]));
            }

            // 3. Load Dynamic Time Slots (THIS CALLS THE BUILDER)
            buildTimeSlotCheckboxes();

        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
    
    private void saveSettings() {
        try {
            configDAO.updateLeadTime((Integer) leadTimeSpinner.getValue());

            for (int i = 0; i < days.length; i++) {
                configDAO.updateDayStatus(days[i], dayChecks[i].isSelected());
            }

            for (JCheckBox cb : timeChecks) {
                configDAO.updateTimeSlotStatus(cb.getText(), cb.isSelected());
            }

            JOptionPane.showMessageDialog(this, "Clinic settings successfully updated!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage());
        }
    }
    private void refreshTimeSlotsUI() {
        buildTimeSlotCheckboxes();
    }

    private void buildTimeSlotCheckboxes() {
        timePanel.removeAll(); 
        timeChecks.clear();    

        // 1. Create the AM Column & its ScrollPane
        JPanel amList = new JPanel();
        amList.setLayout(new BoxLayout(amList, BoxLayout.Y_AXIS));
        amList.setBackground(Color.WHITE);

        JScrollPane amScroll = new JScrollPane(amList);
        amScroll.setBorder(BorderFactory.createTitledBorder("Morning (AM)"));
        amScroll.getVerticalScrollBar().setUnitIncrement(10); // Smoother scrolling

        // 2. Create the PM Column & its ScrollPane
        JPanel pmList = new JPanel();
        pmList.setLayout(new BoxLayout(pmList, BoxLayout.Y_AXIS));
        pmList.setBackground(Color.WHITE);

        JScrollPane pmScroll = new JScrollPane(pmList);
        pmScroll.setBorder(BorderFactory.createTitledBorder("Afternoon/Evening (PM)"));
        pmScroll.getVerticalScrollBar().setUnitIncrement(10);

        try {
            List<String> allSlots = appService.getAllTimeSlots(); 
            String[] activeSlots = appService.getTimeSlots(); 
            java.util.Set<String> activeSet = new java.util.HashSet<>(java.util.Arrays.asList(activeSlots));

            for (String slot : allSlots) {
                JPanel row = createTimeRow(slot, activeSet.contains(slot));

                // Sort into the correct list
                if (slot.contains("AM")) {
                    amList.add(row);
                } else {
                    pmList.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Add the ScrollPanes (not the lists) to the main timePanel
        timePanel.add(amScroll);
        timePanel.add(pmScroll);

        timePanel.revalidate();
        timePanel.repaint();
    }

    // Helper method to keep buildTimeSlotCheckboxes clean
    private JPanel createTimeRow(String slot, boolean isActive) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        row.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JCheckBox cb = new JCheckBox(slot);
        cb.setBackground(Color.WHITE);
        cb.setSelected(isActive);
        timeChecks.add(cb);
        row.add(cb, BorderLayout.WEST);

        JButton delBtn = new JButton("Delete");
        delBtn.setForeground(Color.RED);
        delBtn.setFont(new Font("Arial", Font.PLAIN, 10));
        delBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Delete " + slot + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (configDAO.deleteTimeSlot(slot)) refreshTimeSlotsUI();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        row.add(delBtn, BorderLayout.EAST);

        return row;
    }
    
    private void buildServiceList() {
        servicePanel.removeAll();
        servicePanel.setLayout(new BoxLayout(servicePanel, BoxLayout.Y_AXIS));

        try {
            List<Object[]> services = appService.getFullServiceList(); 

            for (Object[] serviceData : services) {
                String name = (String) serviceData[0];
                
                // --- THE CRASH FIX: Safe Integer Conversion ---
                // Instead of (int), we convert to String then parse, or handle it as an Object
                boolean isActive = serviceData[1] != null && serviceData[1].toString().equals("1");


                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(Color.WHITE);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)));

                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(new Font("Arial", isActive ? Font.BOLD : Font.ITALIC, 13));
                nameLabel.setForeground(isActive ? Color.BLACK : Color.GRAY);
                nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
                row.add(nameLabel, BorderLayout.WEST);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                btnPanel.setBackground(Color.WHITE);

                // 1. Capture the status in a FINAL variable so the lambda can "see" it safely
                final boolean currentActive = isActive; 

                // 2. Setup the button text based on that captured status
                JButton toggleBtn = new JButton(currentActive ? "Deactivate" : "Activate");
                toggleBtn.setFont(new Font("Arial", Font.PLAIN, 10));

                toggleBtn.addActionListener(e -> {
                    try {
                        // 3. Logic: Flip whatever the CURRENT status is
                        boolean targetStatus = !isActive; 
                        if (configDAO.updateServiceStatus(name, targetStatus)) {
                            buildServiceList(); // REFRESH UI
                        }
                    } catch (Exception ex) { 
                        ex.printStackTrace(); 
                    }
                });

                JButton delBtn = new JButton("Delete");
                delBtn.setForeground(Color.RED);
                delBtn.setFont(new Font("Arial", Font.PLAIN, 10));
                delBtn.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(this, "Delete " + name + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            if (configDAO.deleteService(name)) buildServiceList();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                });

                btnPanel.add(toggleBtn);
                btnPanel.add(delBtn);
                row.add(btnPanel, BorderLayout.EAST);
                servicePanel.add(row);
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }

        servicePanel.revalidate();
        servicePanel.repaint();
    }
}