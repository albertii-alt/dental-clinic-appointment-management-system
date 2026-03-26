package com.dentalclinic.admin;

import javax.swing.*;
import javax.swing.border.*;
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
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String currentRole;

    private String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private AppointmentService appService = new AppointmentService();
    private ClinicConfigDAO configDAO = new ClinicConfigDAO();

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
                if (configDAO.addTimeSlot(timeInput, currentAdminId, currentRole)) {
                    newTimeField.setText("");
                    refreshTimeSlotsUI();
                }
            } catch (java.sql.SQLException ex) {
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
                String name = serviceNameField.getText().trim();
                String desc = serviceDescField.getText().trim();
                String priceStr = servicePriceField.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Service Name required.");
                    return;
                }
                if (desc.isEmpty()) desc = "No description provided.";
                double price = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr);

                if (configDAO.addService(name, desc, price, currentAdminId, currentRole)) {
                    serviceNameField.setText("");
                    serviceDescField.setText("");
                    servicePriceField.setText("");
                    buildServiceList();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Check price format: " + ex.getMessage());
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
        try {
            leadTimeSpinner.setValue(appService.getBookingLeadTime());
            List<String> closedDays = appService.getClosedDays();
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
            configDAO.updateLeadTime((Integer) leadTimeSpinner.getValue(), currentAdminId, currentRole);
            for (int i = 0; i < days.length; i++) {
                configDAO.updateDayStatus(days[i], dayChecks[i].isSelected(), currentAdminId, currentRole);
            }
            for (JCheckBox cb : timeChecks) {
                configDAO.updateTimeSlotStatus(cb.getText(), cb.isSelected(), currentAdminId, currentRole);
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
            List<String> allSlots = appService.getAllTimeSlots();
            String[] activeSlots = appService.getTimeSlots();
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
                    if (configDAO.deleteTimeSlot(slot, currentAdminId, currentRole)) {
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
            List<Object[]> services = appService.getFullServiceList();
            for (Object[] serviceData : services) {
                String name = (String) serviceData[0];
                boolean isActive = serviceData[1] != null && serviceData[1].toString().equals("1");

                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(CARD);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
                row.setBorder(new MatteBorder(0, 0, 1, 0, new Color(245, 245, 245)));

                JLabel nameLabel = new JLabel(name);
                nameLabel.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 14));
                nameLabel.setForeground(isActive ? TEXT : TEXT_MUTED);
                nameLabel.setBorder(new EmptyBorder(0, 15, 0, 0));
                row.add(nameLabel, BorderLayout.WEST);

                JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
                btnPanel.setBackground(CARD);

                JButton toggleBtn = new JButton(isActive ? "Disable" : "Enable");
                styleButton(toggleBtn, isActive ? TEXT_MUTED : PRIMARY);
                toggleBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));

                toggleBtn.addActionListener(e -> {
                    try {
                        if (configDAO.updateServiceStatus(name, !isActive, currentAdminId, currentRole)) {
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
                            if (configDAO.deleteService(name, currentAdminId, currentRole)) buildServiceList();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                });

                btnPanel.add(toggleBtn);
                btnPanel.add(delBtn);
                row.add(btnPanel, BorderLayout.EAST);
                servicePanel.add(row);
            }
        } catch (Exception e) { e.printStackTrace(); }
        servicePanel.revalidate();
        servicePanel.repaint();
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
}