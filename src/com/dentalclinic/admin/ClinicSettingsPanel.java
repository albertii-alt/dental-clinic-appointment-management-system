package com.dentalclinic.admin;

import com.dentalclinic.controller.ClinicSettingsController;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import com.dentalclinic.util.Sanitizer;  // ADDED: Import Sanitizer

public class ClinicSettingsPanel extends JPanel {
    private JSpinner leadTimeSpinner;
    private JCheckBox[] dayChecks;
    private List<JCheckBox> timeChecks = new ArrayList<>();
    private JTextField newTimeField;
    private JButton addTimeBtn;
    private JPanel timePanel;
    private JPanel servicePanel;
    private JTextField serviceNameField, serviceDescField, servicePriceField;
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String currentRole;
    private boolean settingsLoaded = false;

    private String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private final ClinicSettingsController clinicSettingsController = new ClinicSettingsController();

    // THEME COLORS
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color DANGER = new Color(231, 76, 60);
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color TEXT = new Color(44, 62, 80);
    private final Color TEXT_MUTED = new Color(127, 140, 141);

    public ClinicSettingsPanel(int adminId, boolean isSuper) {
        this.currentAdminId = adminId;
        this.isSuperAdmin = isSuper;
        this.currentRole = isSuper ? "Super Admin" : "Admin";

        setLayout(new BorderLayout());
        setBackground(BG);

        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG);
        headerPanel.setBorder(new EmptyBorder(30, 40, 15, 40));

        JLabel title = new JLabel("Clinic Configuration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT);
        
        JLabel subtitle = new JLabel("Manage booking rules, operating hours, and available treatments");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_MUTED);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1));
        titleGroup.setOpaque(false);
        titleGroup.add(title);
        titleGroup.add(subtitle);
        headerPanel.add(titleGroup, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // --- SCROLLABLE MAIN CONTENT ---
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BG);
        mainContent.setBorder(new EmptyBorder(10, 40, 10, 40));

        // 1. Lead Time Card
        mainContent.add(createCardPanel("Booking Constraints", createLeadTimePanel()));
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // 2. Days Card
        mainContent.add(createCardPanel("Clinic Operating Days", createDaysPanel()));
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // 3. Time Slots Card
        JPanel timeCardContent = new JPanel(new BorderLayout(0, 15));
        timeCardContent.setBackground(CARD);
        
        timePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        timePanel.setBackground(CARD);
        
        JScrollPane timeScroll = new JScrollPane(timePanel);
        timeScroll.setBorder(null);
        timeScroll.setPreferredSize(new Dimension(0, 300));
        timeScroll.getViewport().setBackground(CARD);

        timeCardContent.add(timeScroll, BorderLayout.CENTER);
        timeCardContent.add(createAddTimePanel(), BorderLayout.SOUTH);
        mainContent.add(createCardPanel("Daily Appointment Slots", timeCardContent));
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // 4. Services Card
        JPanel serviceCardContent = new JPanel(new BorderLayout(0, 15));
        serviceCardContent.setBackground(CARD);

        servicePanel = new JPanel();
        servicePanel.setLayout(new BoxLayout(servicePanel, BoxLayout.Y_AXIS));
        servicePanel.setBackground(CARD);

        JScrollPane serviceScroll = new JScrollPane(servicePanel);
        serviceScroll.setBorder(new LineBorder(new Color(240, 240, 240)));
        serviceScroll.setPreferredSize(new Dimension(0, 250));

        serviceCardContent.add(serviceScroll, BorderLayout.CENTER);
        serviceCardContent.add(createServiceInputPanel(), BorderLayout.SOUTH);
        mainContent.add(createCardPanel("Offered Services & Pricing", serviceCardContent));

        JScrollPane mainScroll = new JScrollPane(mainContent);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        // --- FOOTER SAVE ACTION ---
        JButton saveBtn = new JButton("Apply All Configuration Changes");
        styleButton(saveBtn, SUCCESS);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        saveBtn.setPreferredSize(new Dimension(0, 60));
        saveBtn.addActionListener(e -> saveSettings());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);
        bottom.setBorder(new EmptyBorder(15, 40, 25, 40));
        bottom.add(saveBtn, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);

        buildServiceList();
        loadCurrentSettings();
    }

    // ---------- UI BUILDERS ----------

    private JPanel createLeadTimePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        panel.setBackground(CARD);

        leadTimeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 30, 1));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) leadTimeSpinner.getEditor();
        editor.getTextField().setColumns(3);
        editor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel leadLabel = new JLabel("Minimum advanced booking notice: ");
        leadLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel daysLabel = new JLabel(" days");
        daysLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        panel.add(leadLabel);
        panel.add(leadTimeSpinner);
        panel.add(daysLabel);

        return panel;
    }

    private JPanel createDaysPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 15, 15));
        panel.setBackground(CARD);
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));

        dayChecks = new JCheckBox[7];
        for (int i = 0; i < days.length; i++) {
            dayChecks[i] = new JCheckBox(days[i]);
            dayChecks[i].setBackground(CARD);
            dayChecks[i].setFont(new Font("Segoe UI", Font.BOLD, 14));
            dayChecks[i].setForeground(TEXT);
            panel.add(dayChecks[i]);
        }
        return panel;
    }

    private JPanel createAddTimePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBackground(new Color(250, 251, 252));
        panel.setBorder(new LineBorder(new Color(230, 235, 240), 1, true));

        newTimeField = new JTextField(12);
        newTimeField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        newTimeField.setToolTipText("Format: HH:MM AM/PM");

        addTimeBtn = new JButton("Add Time Slot");
        styleButton(addTimeBtn, PRIMARY);

        addTimeBtn.addActionListener(e -> {
            String timeInput = newTimeField.getText().trim().toUpperCase();
            if (!timeInput.matches("^(0[1-9]|1[0-2]):[0-5][0-9] (AM|PM)$")) {
                JOptionPane.showMessageDialog(this, "Use format: HH:MM AM/PM (e.g., 09:00 AM)");
                return;
            }
            try {
                if (clinicSettingsController.addTimeSlot(timeInput, currentAdminId, currentRole)) {
                    newTimeField.setText("");
                    refreshTimeSlotsUI();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
            }
        });

        panel.add(new JLabel("New Slot:"));
        panel.add(newTimeField);
        panel.add(addTimeBtn);

        return panel;
    }
    
    private JPanel createServiceInputPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(CARD);
        container.setBorder(new EmptyBorder(10, 0, 0, 0));

        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(230, 230, 230));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(250, 251, 252));
        panel.setBorder(new CompoundBorder(
            new LineBorder(new Color(230, 235, 240), 1, true),
            new EmptyBorder(10, 15, 10, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        serviceNameField = new JTextField(12);
        serviceDescField = new JTextField(15);
        servicePriceField = new JTextField(8);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Service Name:"), gbc);
        gbc.gridx = 1; panel.add(serviceNameField, gbc);
        gbc.gridx = 2; panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 3; panel.add(servicePriceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2; panel.add(serviceDescField, gbc);
        
        JButton addServiceBtn = new JButton("Add New Service");
        styleButton(addServiceBtn, PRIMARY);
        gbc.gridx = 3; gbc.gridwidth = 1; panel.add(addServiceBtn, gbc);

        addServiceBtn.addActionListener(e -> {
            try {
                // ==========================================================
                // FIXED: Get raw inputs and apply Sanitizer
                // ==========================================================
                String rawName = serviceNameField.getText().trim();
                String rawDesc = serviceDescField.getText().trim();
                String priceStr = servicePriceField.getText().trim();

                // APPLY SANITIZER to text fields
                String name = Sanitizer.sanitizeTextField(rawName);
                String desc = Sanitizer.sanitizeTextField(rawDesc);

                // Validate name
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(ClinicSettingsPanel.this, "Service Name required.");
                    return;
                }

                // Validate price
                if (!isValidPrice(priceStr)) {
                    JOptionPane.showMessageDialog(ClinicSettingsPanel.this, "Please enter a valid price (0-999999.99).");
                    return;
                }

                double price = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr);

                if (clinicSettingsController.addService(name, desc, price, currentAdminId, currentRole)) {
                    serviceNameField.setText("");
                    serviceDescField.setText("");
                    servicePriceField.setText("");
                    buildServiceList();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ClinicSettingsPanel.this, "Check price format: " + ex.getMessage());
            }
        });

        container.add(separator, BorderLayout.NORTH);
        container.add(panel, BorderLayout.CENTER);
        return container;
    }

    private JPanel createCardPanel(String title, JPanel content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(218, 226, 234), 1, true),
            new EmptyBorder(20, 25, 20, 25)
        ));

        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(PRIMARY);
        lbl.setBorder(new EmptyBorder(0, 0, 15, 0));

        card.add(lbl, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // ---------- ORIGINAL LOGIC (UNCHANGED) ----------

    private void loadCurrentSettings() {
        if (settingsLoaded) return;
        settingsLoaded = true;

        try {
            leadTimeSpinner.setValue(clinicSettingsController.getBookingLeadTime());
            List<String> closedDays = clinicSettingsController.getClosedDays();
            for (int i = 0; i < days.length; i++) {
                dayChecks[i].setSelected(!closedDays.contains(days[i]));
            }
            buildTimeSlotCheckboxes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        try {
            clinicSettingsController.updateLeadTime((Integer) leadTimeSpinner.getValue(), currentAdminId, currentRole);
            for (int i = 0; i < days.length; i++) {
                clinicSettingsController.updateDayStatus(days[i], dayChecks[i].isSelected(), currentAdminId, currentRole);
            }
            for (JCheckBox cb : timeChecks) {
                clinicSettingsController.updateTimeSlotStatus(cb.getText(), cb.isSelected(), currentAdminId, currentRole);
            }
            JOptionPane.showMessageDialog(this, "Clinic settings updated successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void refreshTimeSlotsUI() {
        buildTimeSlotCheckboxes();
    }

    private void buildTimeSlotCheckboxes() {
        timePanel.removeAll();
        timeChecks.clear();

        JPanel amList = createTimeSubPanel("MORNING (AM)");
        JPanel pmList = createTimeSubPanel("AFTERNOON (PM)");

        try {
            List<String> allSlots = clinicSettingsController.getAllTimeSlots();
            String[] activeSlots = clinicSettingsController.getActiveTimeSlots();
            java.util.Set<String> activeSet = new java.util.HashSet<>(java.util.Arrays.asList(activeSlots));

            for (String slot : allSlots) {
                JPanel row = createTimeRow(slot, activeSet.contains(slot));
                if (slot.contains("AM")) amList.add(row);
                else pmList.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        timePanel.add(amList);
        timePanel.add(pmList);
        timePanel.revalidate();
        timePanel.repaint();
    }

    private JPanel createTimeSubPanel(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(TitledBorderFactory.createPaddedBorder(title, TEXT_MUTED));
        return p;
    }

    private JPanel createTimeRow(String slot, boolean isActive) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setBorder(new EmptyBorder(2, 5, 2, 5));

        JCheckBox cb = new JCheckBox(slot);
        cb.setBackground(CARD);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setSelected(isActive);
        timeChecks.add(cb);
        row.add(cb, BorderLayout.WEST);

        JButton delBtn = new JButton("Remove");
        delBtn.setForeground(DANGER);
        delBtn.setContentAreaFilled(false);
        delBtn.setBorderPainted(false);
        delBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        delBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Permanently delete slot " + slot + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (clinicSettingsController.deleteTimeSlot(slot, currentAdminId, currentRole)) {
                        refreshTimeSlotsUI();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage());
                }
            }
        });

        row.add(delBtn, BorderLayout.EAST);
        return row;
    }

    private void buildServiceList() {
        servicePanel.removeAll();
        try {
            List<Object[]> services = clinicSettingsController.getServiceList();
            for (Object[] serviceData : services) {
                String name = (String) serviceData[0];
                boolean isActive = serviceData[1] != null && serviceData[1].toString().equals("1");

                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(CARD);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(245, 245, 245)));

                // ==========================================================
                // FIXED: Escape service name for display
                // ==========================================================
                JLabel nameLabel = new JLabel(Sanitizer.escapeForHTML(name));
                nameLabel.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14));
                nameLabel.setForeground(isActive ? TEXT : TEXT_MUTED);
                nameLabel.setBorder(new EmptyBorder(0, 15, 0, 0));
                row.add(nameLabel, BorderLayout.WEST);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
                btnPanel.setBackground(CARD);

                // EDIT BUTTON
                JButton editBtn = new JButton("Edit");
                styleButton(editBtn, new Color(52, 152, 219));
                editBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
                editBtn.addActionListener(e -> {
                    showEditServiceDialog(name);
                });

                JButton toggleBtn = new JButton(isActive ? "Disable" : "Enable");
                styleButton(toggleBtn, isActive ? TEXT_MUTED : PRIMARY);
                toggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));

                toggleBtn.addActionListener(e -> {
                    try {
                        if (clinicSettingsController.updateServiceStatus(name, !isActive, currentAdminId, currentRole)) {
                            buildServiceList();
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                JButton delBtn = new JButton("Delete");
                delBtn.setForeground(DANGER);
                delBtn.setContentAreaFilled(false);
                delBtn.addActionListener(e -> {
                    if (JOptionPane.showConfirmDialog(this, "Delete " + name + "?") == JOptionPane.YES_OPTION) {
                        try {
                            if (clinicSettingsController.deleteService(name, currentAdminId, currentRole)) buildServiceList();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                });

                btnPanel.add(editBtn);
                btnPanel.add(toggleBtn);
                btnPanel.add(delBtn);
                row.add(btnPanel, BorderLayout.EAST);
                servicePanel.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        servicePanel.revalidate();
        servicePanel.repaint();
    }

    /**
     * Show Edit Service Dialog
     */
    private void showEditServiceDialog(String serviceName) {
        // Fetch current service details
        String currentName = serviceName;
        String currentDesc = "";
        double currentPrice = 0.0;
        
        try {
            Object[] serviceDetails = clinicSettingsController.getServiceDetailsByName(serviceName);
            currentDesc = serviceDetails[0] != null ? serviceDetails[0].toString() : "";
            currentPrice = serviceDetails[1] instanceof Number ? ((Number) serviceDetails[1]).doubleValue() : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create dialog
        JDialog editDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Service", true);
        editDialog.setLayout(new BorderLayout());
        editDialog.setSize(450, 350);
        editDialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        panel.add(new JLabel("Service Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(currentName, 20);
        panel.add(nameField, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setText(currentDesc);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descArea);
        panel.add(descScroll, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        JTextField priceField = new JTextField(String.format("%.2f", currentPrice), 10);
        panel.add(priceField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save Changes");
        JButton cancelBtn = new JButton("Cancel");
        
        styleButton(saveBtn, SUCCESS);
        styleButton(cancelBtn, TEXT_MUTED);
        
        saveBtn.addActionListener(e -> {
            // ==========================================================
            // FIXED: Apply Sanitizer to edit dialog inputs
            // ==========================================================
            String rawNewName = nameField.getText().trim();
            String rawNewDesc = descArea.getText().trim();
            String priceStr = priceField.getText().trim();
            
            String newName = Sanitizer.sanitizeTextField(rawNewName);
            String newDesc = Sanitizer.sanitizeTextField(rawNewDesc);
            
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Service name cannot be empty.");
                return;
            }
            
            if (!isValidPrice(priceStr)) {
                JOptionPane.showMessageDialog(editDialog, "Please enter a valid price (0-999999.99).");
                return;
            }
            
            double newPrice = Double.parseDouble(priceStr);
            
            try {
                if (clinicSettingsController.updateService(serviceName, newName, newDesc, newPrice, currentAdminId, currentRole)) {
                    JOptionPane.showMessageDialog(editDialog, "Service updated successfully!");
                    editDialog.dispose();
                    buildServiceList();
                } else {
                    JOptionPane.showMessageDialog(editDialog, "Failed to update service.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editDialog, "Error: " + ex.getMessage());
            }
        });
        
        cancelBtn.addActionListener(e -> editDialog.dispose());
        
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        
        editDialog.add(panel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }
    
    // Helper for titled borders with custom padding
    private static class TitledBorderFactory {
        public static Border createPaddedBorder(String title, Color color) {
            TitledBorder tb = BorderFactory.createTitledBorder(
                new LineBorder(new Color(240, 240, 240)), title);
            tb.setTitleColor(color);
            tb.setTitleFont(new Font("Segoe UI", Font.BOLD, 11));
            return new CompoundBorder(tb, new EmptyBorder(10, 10, 10, 10));
        }
    }
    
    // REMOVED: Old sanitizeInput() method - replaced with Sanitizer utility
    // private String sanitizeInput(String input) { ... }  // DELETED

    // SECURITY: Validate service price (kept as-is)
    private boolean isValidPrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return true;
        try {
            double price = Double.parseDouble(priceStr);
            return price >= 0 && price <= 999999.99;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public void cleanup() {
        System.out.println("Cleaning up ClinicSettingsPanel...");
    }
}
