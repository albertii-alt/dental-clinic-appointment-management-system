// UI ENHANCED VERSION WITH SECURITY FIXES
package com.dentalclinic.view.staff;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.dto.appointment.AppointmentRequest;
import com.dentalclinic.dto.appointment.BookingResult;
import com.dentalclinic.util.Sanitizer;  // ADDED: Import Sanitizer

public class StaffBookAppointmentPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static InitialDataCacheEntry initialDataCache;
    private static final Map<String, SearchCacheEntry> SEARCH_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, SlotCacheEntry> SLOT_CACHE = new ConcurrentHashMap<>();

    private AppointmentController appointmentController = new AppointmentController();
    private JLabel stepLabel;
    private JTextField searchField;
    private JComboBox<String> patientResultsCombo;
    private List<Object[]> currentSearchResults;
    
    private JTextField fNameField, lNameField, ageField, contactField;
    private JComboBox<String> serviceTypeCombo, timeSlotCombo;
    private JDateChooser appointmentDatePicker;
    private int selectedPatientID = -1;
    private SwingWorker<InitialData, Void> initialDataWorker;
    private SwingWorker<List<Object[]>, Void> searchWorker;
    private SwingWorker<List<String>, Void> slotWorker;
    private long searchRequestId = 0;
    private long slotRequestId = 0;
    private final java.util.Set<Integer> closedDaysOfWeek = new java.util.HashSet<>();
    private boolean dateEvaluatorAdded = false;

    // SECURITY: Input limits
    private static final int MAX_CONTACT_LENGTH = 11;
    private static final int MAX_AGE = 120;
    private static final int MIN_AGE = 0;

    private static class InitialData {
        private final String[] services;
        private final int leadTime;
        private final List<String> closedDays;

        private InitialData(String[] services, int leadTime, List<String> closedDays) {
            this.services = services;
            this.leadTime = leadTime;
            this.closedDays = closedDays;
        }
    }

    private static class InitialDataCacheEntry {
        private final InitialData data;
        private final long createdAtMs;

        private InitialDataCacheEntry(InitialData data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    private static class SearchCacheEntry {
        private final List<Object[]> rows;
        private final long createdAtMs;

        private SearchCacheEntry(List<Object[]> rows, long createdAtMs) {
            this.rows = rows;
            this.createdAtMs = createdAtMs;
        }
    }

    private static class SlotCacheEntry {
        private final List<String> slots;
        private final long createdAtMs;

        private SlotCacheEntry(List<String> slots, long createdAtMs) {
            this.slots = slots;
            this.createdAtMs = createdAtMs;
        }
    }

    // THEME
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color TEXT = new Color(44, 62, 80);
    private final int SPACING = 12;

    public StaffBookAppointmentPanel() {
        // Initialize Components
        searchField = new JTextField();
        patientResultsCombo = new JComboBox<>();
        fNameField = new JTextField();
        contactField = new JTextField();
        ageField = new JTextField();
        timeSlotCombo = new JComboBox<>();
        serviceTypeCombo = new JComboBox<>();
        appointmentDatePicker = new JDateChooser();

        setLayout(new GridBagLayout());
        setBackground(BG);

        // Main Card Container
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setPreferredSize(new Dimension(500, 750));
        container.setBackground(CARD);
        container.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 220), 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));

        // TITLE
        JLabel title = new JLabel("Staff Booking Portal");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(title);
        
        JLabel subtitle = new JLabel("Create and auto-approve appointments");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(subtitle);
        
        container.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // -------- STEP 1: PATIENT SELECTION --------
        container.add(createSectionLabel("Step 1: Patient Information"));
        container.add(Box.createRigidArea(new Dimension(0, 8)));
        
        container.add(createLabelOnly("Search Patient Name:"));
        container.add(createInput(searchField));
        container.add(Box.createRigidArea(new Dimension(0, 5)));
        container.add(createCombo(patientResultsCombo));
        
        container.add(Box.createRigidArea(new Dimension(0, SPACING)));
        container.add(createFieldWithLabel("Selected Patient Name", fNameField, false));
        container.add(createFieldWithLabel("Contact Number", contactField, true));

        container.add(Box.createRigidArea(new Dimension(0, 10)));
        container.add(createDivider());
        container.add(Box.createRigidArea(new Dimension(0, 10)));

        // -------- STEP 2: SCHEDULE --------
        container.add(createSectionLabel("Step 2: Service & Schedule"));
        container.add(Box.createRigidArea(new Dimension(0, 8)));

        container.add(createLabelOnly("Select Service:"));
        container.add(createCombo(serviceTypeCombo));
        
        container.add(Box.createRigidArea(new Dimension(0, SPACING)));
        
        container.add(createLabelOnly("Appointment Date:"));
        appointmentDatePicker.setDateFormatString("MMMM d, yyyy");
        container.add(createDatePicker(appointmentDatePicker));

        container.add(Box.createRigidArea(new Dimension(0, SPACING)));
        
        container.add(createLabelOnly("Available Time Slot:"));
        container.add(createCombo(timeSlotCombo));

        // Logic for Date restrictions
        setupCalendarFilterEvaluator();

        // BUTTON
        container.add(Box.createRigidArea(new Dimension(0, 30)));
        JButton confirmBtn = new JButton("Confirm & Approve Appointment");
        styleButton(confirmBtn, SUCCESS);
        confirmBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        container.add(confirmBtn);

        // LISTENERS
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                refreshPatientDropdownAsync(searchField.getText(), false);
            }
        });

        patientResultsCombo.addActionListener(e -> selectPatient());
        appointmentDatePicker.addPropertyChangeListener("date", evt -> refreshSlots(false));
        confirmBtn.addActionListener(e -> handleStaffBooking());

        // SECURITY: Add input validation listeners
        addContactValidation(contactField);
        addAgeValidation(ageField);

        loadInitialDataAsync(false);
        refreshPatientDropdownAsync("", false);

        GridBagConstraints gbc = new GridBagConstraints();
        add(container, gbc);
    }

    // SECURITY: Contact number validation (digits only)
    private void addContactValidation(JTextField field) {
        field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c) || field.getText().length() >= MAX_CONTACT_LENGTH) {
                    evt.consume();
                }
            }
        });
    }
    
    // SECURITY: Age validation
    private void addAgeValidation(JTextField field) {
        field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                char c = evt.getKeyChar();
                if (!Character.isDigit(c)) {
                    evt.consume();
                }
            }
        });
    }
    
    // REMOVED: Old sanitizeInput() method - replaced with Sanitizer utility
    // private String sanitizeInput(String input) { ... }  // DELETED
    
    // ==========================================================
    // FIXED: Updated validation to use Sanitizer where appropriate
    // ==========================================================
    
    // SECURITY: Validate age (kept as-is since it's numeric validation)
    private boolean isValidAge(String ageStr) {
        if (ageStr == null || ageStr.isEmpty()) return true;
        try {
            int age = Integer.parseInt(ageStr);
            return age >= MIN_AGE && age <= MAX_AGE;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    // SECURITY: Validate contact (kept as-is, but will also sanitize)
    private boolean isValidContact(String contact) {
        return contact != null && contact.matches("\\d{7,11}");
    }

    // ---------- UI HELPERS ----------

    private JLabel createSectionLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel createLabelOnly(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 0, 4, 0));
        return lbl;
    }

    private JComponent createInput(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200)),
                new EmptyBorder(5, 10, 5, 10)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    private JComponent createCombo(JComboBox<?> combo) {
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        combo.setBackground(Color.WHITE);
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        return combo;
    }

    private JComponent createDatePicker(JDateChooser picker) {
        picker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        picker.setAlignmentX(Component.LEFT_ALIGNMENT);
        return picker;
    }

    private JPanel createFieldWithLabel(String label, JTextField field, boolean editable) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(createLabelOnly(label));
        panel.add(createInput(field));
        field.setEditable(editable);
        
        if (!editable) {
            field.setBackground(new Color(245, 245, 245));
        }

        return panel;
    }

    private JSeparator createDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 230, 230));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private void styleButton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
    }

    // ---------- LOGIC ----------

    private void selectPatient() {
        int idx = patientResultsCombo.getSelectedIndex();
        if (idx >= 0 && currentSearchResults != null && idx < currentSearchResults.size()) {
            Object[] p = currentSearchResults.get(idx);
            selectedPatientID = (int) p[0];
            // ==========================================================
            // FIXED: Use Sanitizer for patient name and contact
            // ==========================================================
            String patientName = (String) p[1];
            fNameField.setText(Sanitizer.sanitizeName(patientName));
            String contact = (String) p[4];
            contactField.setText(Sanitizer.sanitizePhone(contact));

            java.util.Date dob = (java.util.Date) p[2];
            if (dob != null) {
                java.time.LocalDate birthDate = java.time.Instant.ofEpochMilli(dob.getTime())
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
                int age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
                ageField.setText(String.valueOf(age));
            }
        }
    }

    private void refreshPatientDropdownAsync(String query, boolean forceRefresh) {
        String trimmedQuery = query == null ? "" : query.trim();
        String cacheKey = trimmedQuery.toLowerCase();

        SearchCacheEntry cached = SEARCH_CACHE.get(cacheKey);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            applyPatientSearchResults(cached.rows);
            return;
        }

        if (searchWorker != null && !searchWorker.isDone()) {
            searchWorker.cancel(true);
        }

        final long requestId = ++searchRequestId;
        patientResultsCombo.setEnabled(false);
        patientResultsCombo.setModel(new DefaultComboBoxModel<>(new String[]{"Searching..."}));

        searchWorker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                if (trimmedQuery.isEmpty()) {
                    return appointmentController.searchPatientsByName("");
                }
                return appointmentController.searchPatientsByName(trimmedQuery);
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != searchRequestId) {
                    return;
                }

                try {
                    List<Object[]> rows = get();
                    SEARCH_CACHE.put(cacheKey, new SearchCacheEntry(new ArrayList<>(rows), System.currentTimeMillis()));
                    applyPatientSearchResults(rows);
                } catch (Exception e) {
                    e.printStackTrace();
                    patientResultsCombo.setModel(new DefaultComboBoxModel<>(new String[]{"Search failed"}));
                } finally {
                    patientResultsCombo.setEnabled(true);
                }
            }
        };

        searchWorker.execute();
    }

    private void applyPatientSearchResults(List<Object[]> rows) {
        currentSearchResults = rows;
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (Object[] p : currentSearchResults) {
            String displayName = Sanitizer.sanitizeName((String) p[1]) + " (ID: " + p[0] + ")";
            model.addElement(displayName);
        }
        patientResultsCombo.setModel(model);
    }

    private void refreshSlots(boolean forceRefresh) {
        if (appointmentDatePicker.getDate() == null) return;

        String dateKey = new SimpleDateFormat("yyyy-MM-dd").format(appointmentDatePicker.getDate());
        SlotCacheEntry cached = SLOT_CACHE.get(dateKey);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            applySlots(cached.slots);
            return;
        }

        if (slotWorker != null && !slotWorker.isDone()) {
            slotWorker.cancel(true);
        }

        final long requestId = ++slotRequestId;
        timeSlotCombo.setEnabled(false);
        timeSlotCombo.removeAllItems();
        timeSlotCombo.addItem("Loading...");

        java.util.Date selectedDate = appointmentDatePicker.getDate();
        slotWorker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return appointmentController.getAvailableSlotsForDate(selectedDate);
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != slotRequestId) {
                    return;
                }

                try {
                    List<String> available = get();
                    SLOT_CACHE.put(dateKey, new SlotCacheEntry(new ArrayList<>(available), System.currentTimeMillis()));
                    applySlots(available);
                } catch (Exception e) {
                    e.printStackTrace();
                    timeSlotCombo.removeAllItems();
                    timeSlotCombo.addItem("Failed to load slots");
                } finally {
                    timeSlotCombo.setEnabled(true);
                }
            }
        };

        slotWorker.execute();
    }

    private void applySlots(List<String> available) {
        timeSlotCombo.removeAllItems();
        if (available == null || available.isEmpty()) {
            timeSlotCombo.addItem("Fully Booked");
            return;
        }
        for (String s : available) {
            timeSlotCombo.addItem(s);
        }
    }

    private void loadInitialDataAsync(boolean forceRefresh) {
        if (!forceRefresh && initialDataCache != null && System.currentTimeMillis() - initialDataCache.createdAtMs <= CACHE_TTL_MS) {
            applyInitialData(initialDataCache.data);
            return;
        }

        if (initialDataWorker != null && !initialDataWorker.isDone()) {
            initialDataWorker.cancel(true);
        }

        serviceTypeCombo.setEnabled(false);
        searchField.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        initialDataWorker = new SwingWorker<InitialData, Void>() {
            @Override
            protected InitialData doInBackground() throws Exception {
                String[] services = appointmentController.getServiceList();
                int leadTime = appointmentController.getBookingLeadTime();
                List<String> closedDays = appointmentController.getClosedDays();
                return new InitialData(services, leadTime, closedDays);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    InitialData data = get();
                    initialDataCache = new InitialDataCacheEntry(data, System.currentTimeMillis());
                    applyInitialData(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    appointmentDatePicker.setMinSelectableDate(new java.util.Date());
                } finally {
                    serviceTypeCombo.setEnabled(true);
                    searchField.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        initialDataWorker.execute();
    }

    private void applyInitialData(InitialData data) {
        if (data.services != null) {
            serviceTypeCombo.setModel(new DefaultComboBoxModel<>(data.services));
        }

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, data.leadTime);
        appointmentDatePicker.setMinSelectableDate(cal.getTime());

        updateClosedDaysSet(data.closedDays);
        refreshSlots(false);
    }

    private void setupCalendarFilterEvaluator() {
        if (dateEvaluatorAdded) {
            return;
        }

        appointmentDatePicker.getJCalendar().getDayChooser().addDateEvaluator(new com.toedter.calendar.IDateEvaluator() {
            @Override
            public boolean isInvalid(java.util.Date date) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(date);
                return closedDaysOfWeek.contains(cal.get(java.util.Calendar.DAY_OF_WEEK));
            }

            @Override public boolean isSpecial(java.util.Date date) { return false; }
            @Override public Color getSpecialForegroundColor() { return null; }
            @Override public Color getSpecialBackroundColor() { return null; }
            @Override public String getSpecialTooltip() { return null; }
            @Override public Color getInvalidForegroundColor() { return Color.RED; }
            @Override public Color getInvalidBackroundColor() { return new Color(240, 240, 240); }
            @Override public String getInvalidTooltip() { return "Clinic Closed"; }
        });

        dateEvaluatorAdded = true;
    }

    private void updateClosedDaysSet(List<String> closedDayNames) {
        closedDaysOfWeek.clear();
        if (closedDayNames == null) {
            return;
        }

        Map<String, Integer> dayMap = new HashMap<>();
        dayMap.put("SUNDAY", java.util.Calendar.SUNDAY);
        dayMap.put("MONDAY", java.util.Calendar.MONDAY);
        dayMap.put("TUESDAY", java.util.Calendar.TUESDAY);
        dayMap.put("WEDNESDAY", java.util.Calendar.WEDNESDAY);
        dayMap.put("THURSDAY", java.util.Calendar.THURSDAY);
        dayMap.put("FRIDAY", java.util.Calendar.FRIDAY);
        dayMap.put("SATURDAY", java.util.Calendar.SATURDAY);

        for (String day : closedDayNames) {
            if (day == null) {
                continue;
            }
            Integer mapped = dayMap.get(day.trim().toUpperCase());
            if (mapped != null) {
                closedDaysOfWeek.add(mapped);
            }
        }

        appointmentDatePicker.getJCalendar().revalidate();
        appointmentDatePicker.getJCalendar().repaint();
    }

    private void handleStaffBooking() {
        if (selectedPatientID == -1) {
            JOptionPane.showMessageDialog(this, "Please select a patient first.");
            return;
        }
        if (appointmentDatePicker.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Please select a date.");
            return;
        }

        try {
            // SECURITY: Validate age
            int ageValue = 0;
            String ageText = ageField.getText().trim();
            if (!ageText.isEmpty()) {
                if (!isValidAge(ageText)) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid age (0-120).");
                    return;
                }
                ageValue = Integer.parseInt(ageText);
            }
            
            // ==========================================================
            // FIXED: Get raw contact and sanitize with Sanitizer
            // ==========================================================
            String rawContact = contactField.getText().trim();
            if (!isValidContact(rawContact)) {
                JOptionPane.showMessageDialog(this, "Please enter a valid contact number (7-11 digits).");
                return;
            }
            
            // APPLY SANITIZER to contact
            String contact = Sanitizer.sanitizePhone(rawContact);

            AppointmentRequest request = new AppointmentRequest(
                selectedPatientID,
                (String) serviceTypeCombo.getSelectedItem(),
                appointmentDatePicker.getDate(),
                (String) timeSlotCombo.getSelectedItem(),
                ageValue,
                contact
            );
            BookingResult result = appointmentController.bookAndApproveByStaff(request);
            if (result.isSuccess()) {
                JOptionPane.showMessageDialog(this, result.getMessage());
                if (appointmentDatePicker.getDate() != null) {
                    String key = new SimpleDateFormat("yyyy-MM-dd").format(appointmentDatePicker.getDate());
                    SLOT_CACHE.remove(key);
                    refreshSlots(true);
                }
            } else {
                JOptionPane.showMessageDialog(this, result.getMessage());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid age format.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    public void cleanup() {
        if (appointmentDatePicker != null) {
            appointmentDatePicker.getJCalendar().setVisible(false);
        }
    }
}
