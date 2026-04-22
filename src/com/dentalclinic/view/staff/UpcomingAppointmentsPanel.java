package com.dentalclinic.view.staff;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.controller.PatientController;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.util.EmailUtil;
import com.dentalclinic.util.UserSession;
import com.toedter.calendar.JDateChooser;

public class UpcomingAppointmentsPanel extends JPanel {
    private static final long CACHE_TTL_MS = 30000;
    private static final String CACHE_KEY = "UPCOMING_APPOINTMENTS";
    private static final Map<String, CacheEntry> UPCOMING_CACHE = new ConcurrentHashMap<>();

    private JTable table;
    private DefaultTableModel model;
    private final AppointmentController appointmentController = new AppointmentController();
    private final PatientController patientController = new PatientController();
    private SwingWorker<List<Object[]>, Void> loadWorker;
    private long loadRequestId = 0;

    private static class CacheEntry {
        private final List<Object[]> data;
        private final long createdAtMs;

        private CacheEntry(List<Object[]> data, long createdAtMs) {
            this.data = data;
            this.createdAtMs = createdAtMs;
        }
    }

    // THEME SYNC
    private final Color BG = new Color(245, 247, 250);
    private final Color CARD = Color.WHITE;
    private final Color PRIMARY = new Color(41, 128, 185);
    private final Color SUCCESS = new Color(39, 174, 96);
    private final Color WARNING = new Color(243, 156, 18);
    private final Color TEXT = new Color(44, 62, 80);
    private final Color BORDER_COLOR = new Color(220, 220, 220);

    public UpcomingAppointmentsPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- THE MAIN CARD ---
        JPanel cardContainer = new JPanel(new BorderLayout(0, 20));
        cardContainer.setBackground(CARD);
        cardContainer.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // HEADER SECTION
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(CARD);
        
        JLabel title = new JLabel("Confirmed Upcoming Appointments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(PRIMARY);
        
        JLabel subtitle = new JLabel("Manage, view, and send reminders for approved patient schedules.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Color.GRAY);
        
        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setBackground(CARD);
        titleBox.add(title);
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.WEST);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SYNC_ALT, 13, Color.WHITE));
        refreshBtn.setBackground(PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        refreshBtn.addActionListener(e -> { invalidateCache(); loadUpcomingData(true); });
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnWrapper.setBackground(CARD);
        btnWrapper.add(refreshBtn);
        header.add(btnWrapper, BorderLayout.EAST);

        cardContainer.add(header, BorderLayout.NORTH);

        // --- TABLE SETUP (Added Action Column) ---
        String[] columns = {"App ID", "Patient ID", "Patient Name", "Service", "Date", "Time", "Status", "Action"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { 
                // Only allow the Action column to be editable (for button click)
                return column == 7;
            }
        };

        table = new JTable(model);
        styleTable(table);
        
        // Hide ID columns
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(1).setMinWidth(0);
        table.getColumnModel().getColumn(1).setMaxWidth(0);
        
        // Set column widths
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(7).setPreferredWidth(120);

        // Add Button Renderer and Editor for Action column
        table.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1 && row < model.getRowCount()) {
                        int appId = (int) model.getValueAt(row, 0);
                        int pId = (int) model.getValueAt(row, 1);
                        showUpcomingDetailModal(appId, pId);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        cardContainer.add(scrollPane, BorderLayout.CENTER);

        add(cardContainer, BorderLayout.CENTER);
        loadUpcomingData(false);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(232, 241, 249));
        table.setSelectionForeground(TEXT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setBackground(PRIMARY);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 45));
        
        // Status Color Renderer
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setForeground(SUCCESS);
                setFont(getFont().deriveFont(Font.BOLD));
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void loadUpcomingData() {
        loadUpcomingData(false);
    }

    private void loadUpcomingData(boolean forceRefresh) {
        CacheEntry cached = UPCOMING_CACHE.get(CACHE_KEY);
        if (!forceRefresh && cached != null && System.currentTimeMillis() - cached.createdAtMs <= CACHE_TTL_MS) {
            renderRows(cached.data);
            return;
        }

        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }

        final long requestId = ++loadRequestId;
        table.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        loadWorker = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> rows = new ArrayList<>();
                List<Appointment> upcoming = appointmentController.getUpcomingAppointments();
                for (Appointment a : upcoming) {
                    Patient p = patientController.getPatientById(a.getPatientId());
                    String fullName = p.getFirstName() + " " + p.getLastName();
                    rows.add(new Object[]{
                        a.getAppointmentId(),
                        a.getPatientId(),
                        fullName,
                        a.getServiceType(),
                        a.getAppointmentDate(),
                        a.getAppointmentTime(),
                        a.getStatus(),
                        "Send Reminder"
                    });
                }
                return rows;
            }

            @Override
            protected void done() {
                if (isCancelled() || requestId != loadRequestId) {
                    return;
                }

                try {
                    List<Object[]> rows = get();
                    UPCOMING_CACHE.put(CACHE_KEY, new CacheEntry(new ArrayList<>(rows), System.currentTimeMillis()));
                    renderRows(rows);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UpcomingAppointmentsPanel.this,
                            "Error loading upcoming appointments: " + e.getMessage(),
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    table.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };

        loadWorker.execute();
    }

    private void renderRows(List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private void invalidateCache() {
        UPCOMING_CACHE.remove(CACHE_KEY);
    }

    private void showUpcomingDetailModal(int appId, int pId) {
        try {
            Patient p = patientController.getPatientById(pId);
            List<Appointment> history = appointmentController.getPatientAppointmentHistory(pId);
            Appointment app = history.stream().filter(a -> a.getAppointmentId() == appId).findFirst().orElse(null);

            if (app == null) return;

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(CARD);
            detailPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            // APPOINTMENT SECTION
            detailPanel.add(createHeaderLabel("APPOINTMENT SUMMARY"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createCompactLabel("Service Type: ", app.getServiceType()));
            detailPanel.add(createCompactLabel("Date: ", app.getAppointmentDate().toString()));
            detailPanel.add(createCompactLabel("Time Slot: ", app.getAppointmentTime()));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statusLbl.setForeground(SUCCESS);
            detailPanel.add(statusLbl);

            detailPanel.add(Box.createVerticalStrut(20));

            // PATIENT SECTION
            detailPanel.add(createHeaderLabel("PATIENT INFORMATION"));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            detailPanel.add(createCompactLabel("Full Name: ", p.getFirstName() + " " + p.getLastName()));
            detailPanel.add(createCompactLabel("Age: ", String.valueOf(app.getAgeAtVisit())));
            detailPanel.add(createCompactLabel("Contact No: ", app.getContactAtVisit()));
            detailPanel.add(createCompactLabel("Full Address: ", p.getAddress()));

            String[] options = {"Send Reminder", "Reschedule", "Cancel Appointment", "Close"};
            int choice = JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Detail Record",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[3]
            );

            if (choice == 0) { 
                sendManualReminder(app, p);
            } else if (choice == 1) { 
                openRescheduleDialog(appId);
            } else if (choice == 2) { 
                handleStaffCancellation(appId);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    /**
    * Send manual reminder for an appointment with warning for early reminders
    */
   private void sendManualReminder(Appointment appointment, Patient patient) {
       if (patient.getEmail() == null || patient.getEmail().isEmpty()) {
           JOptionPane.showMessageDialog(this, 
               "This patient does not have an email address on file.\nCannot send reminder.",
               "No Email",
               JOptionPane.WARNING_MESSAGE);
           return;
       }

       // Calculate days until appointment
       java.time.LocalDate appointmentDate = java.time.Instant
               .ofEpochMilli(appointment.getAppointmentDate().getTime())
               .atZone(java.time.ZoneId.systemDefault())
               .toLocalDate();
       java.time.LocalDate today = java.time.LocalDate.now();
       long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, appointmentDate);

       // Build warning message based on days until appointment
       String warningMessage = "";
       String warningLevel = "";

       if (daysUntil < 0) {
           // Appointment already passed
           JOptionPane.showMessageDialog(this,
               "This appointment has already passed.\nCannot send reminder for past appointments.",
               "Past Appointment",
               JOptionPane.ERROR_MESSAGE);
           return;
       } else if (daysUntil == 0) {
           warningLevel = "SAME DAY";
           warningMessage = "[!] This appointment is TODAY.\n" +
                            "The patient may already be on their way.\n\n";
       } else if (daysUntil == 1) {
           warningLevel = "STANDARD";
           // No warning needed - this is the ideal time
       } else if (daysUntil <= 3) {
           warningLevel = "EARLY";
           warningMessage = "[!] This appointment is " + daysUntil + " days away.\n" +
                            "Sending a reminder this early may be less effective.\n\n" +
                            "Standard practice is to send reminders 1 day before.\n\n";
       } else if (daysUntil <= 7) {
           warningLevel = "VERY EARLY";
           warningMessage = "⚠️[!] This appointment is " + daysUntil + " days away.\n\n" +
                            "This is VERY early for a reminder.\n" +
                            "The patient may forget again by the appointment date.\n\n" +
                            "Are you sure you want to send this now?\n\n";
       } else {
           warningLevel = "TOO EARLY";
           warningMessage = "🔴🔴🔴 WARNING: This appointment is " + daysUntil + " days away!\n\n" +
                            "Sending a reminder this early is NOT recommended.\n" +
                            "The patient will likely forget by the appointment date.\n\n" +
                            "Only send this if the patient specifically requested it.\n\n";
       }

       // Build the confirmation message
       String confirmMessage = "Send reminder to:\n\n" +
           "Patient: " + patient.getFirstName() + " " + patient.getLastName() + "\n" +
           "Email: " + patient.getEmail() + "\n" +
           "Service: " + appointment.getServiceType() + "\n" +
           "Date: " + appointment.getAppointmentDate() + "\n" +
           "Time: " + appointment.getAppointmentTime() + "\n" +
           "Days until appointment: " + daysUntil + "\n\n";

       // Add warning if applicable
       if (!warningMessage.isEmpty()) {
           confirmMessage += warningMessage;
       }

       confirmMessage += "Proceed?";

       // Set dialog title based on warning level
       String dialogTitle;
       int optionType;

       switch (warningLevel) {
           case "TOO EARLY":
               dialogTitle = "[!!!] EXTREME CAUTION [!!!]";
               optionType = JOptionPane.OK_CANCEL_OPTION;
               break;
           case "VERY EARLY":
               dialogTitle = "[!!] Early Reminder Warning [!!]";
               optionType = JOptionPane.OK_CANCEL_OPTION;
               break;
           case "EARLY":
               dialogTitle = "[!] Early Reminder Warning";
               optionType = JOptionPane.YES_NO_OPTION;
               break;
           case "SAME DAY":
               dialogTitle = "[!] Same Day Reminder";
               optionType = JOptionPane.YES_NO_OPTION;
               break;
           default:
               dialogTitle = "Send Reminder";
               optionType = JOptionPane.YES_NO_OPTION;
               break;
       }

       int confirm;
       if (warningLevel.equals("TOO EARLY") || warningLevel.equals("VERY EARLY")) {
           // Use OK/CANCEL for extreme cases (more emphasis)
           confirm = JOptionPane.showConfirmDialog(this,
               confirmMessage,
               dialogTitle,
               JOptionPane.OK_CANCEL_OPTION,
               JOptionPane.WARNING_MESSAGE);
           // For OK/CANCEL, OK_OPTION = 0, CANCEL_OPTION = 2
           if (confirm != JOptionPane.OK_OPTION) {
               return;
           }
       } else {
           confirm = JOptionPane.showConfirmDialog(this,
               confirmMessage,
               dialogTitle,
               optionType,
               JOptionPane.QUESTION_MESSAGE);
           if (confirm != JOptionPane.YES_OPTION) {
               return;
           }
       }

       // Send the reminder
       try {
           int actorId = UserSession.getUserId();
           String actorRole = UserSession.getUserRole();

           EmailUtil.sendAppointmentReminderWithActor(
               actorId, actorRole,
               patient.getFirstName() + " " + patient.getLastName(),
               patient.getEmail(),
               appointment.getServiceType(),
               appointment.getAppointmentDate().toString(),
               appointment.getAppointmentTime()
           );

           // Show success message with warning level note if applicable
           String successMessage = "Reminder sent successfully to:\n" + patient.getEmail();
           if (warningLevel.equals("TOO EARLY")) {
               successMessage += "\n\n⚠️ Note: This reminder was sent very early (" + daysUntil + " days before).\n" +
                                 "Consider sending another reminder 1 day before the appointment.";
           } else if (warningLevel.equals("VERY EARLY")) {
               successMessage += "\n\nNote: This reminder was sent " + daysUntil + " days early.\n" +
                                 "You may want to send another reminder closer to the date.";
           }

           JOptionPane.showMessageDialog(this,
               successMessage,
               "Reminder Sent",
               JOptionPane.INFORMATION_MESSAGE);

       } catch (Exception e) {
           JOptionPane.showMessageDialog(this,
               "Failed to send reminder: " + e.getMessage(),
               "Error",
               JOptionPane.ERROR_MESSAGE);
       }
   }
    

    private JLabel createHeaderLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(PRIMARY);
        return lbl;
    }

    private JLabel createCompactLabel(String title, String value) {
        JLabel label = new JLabel("<html><b style='color:#2c3e50'>" + title + "</b> " + value + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        return label;
    }
    
    private void openRescheduleDialog(int appId) {
        JDialog rescheduleDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Reschedule", true);
        rescheduleDialog.setLayout(new BorderLayout());

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainContainer.setBackground(BG);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
        Dimension inputSize = new Dimension(300, 40); 

        autoAddLeftLabel(mainContainer, "Select New Date:", labelFont);
        mainContainer.add(Box.createVerticalStrut(5));

        JDateChooser dateChooser = new JDateChooser();
        dateChooser.setPreferredSize(inputSize);
        dateChooser.setMaximumSize(inputSize); 
        dateChooser.setMinSelectableDate(new java.util.Date());
        mainContainer.add(dateChooser);

        mainContainer.add(Box.createVerticalStrut(20));

        autoAddLeftLabel(mainContainer, "Available Time Slots:", labelFont);
        mainContainer.add(Box.createVerticalStrut(5));

        DefaultComboBoxModel<String> timeModel = new DefaultComboBoxModel<>(new String[]{"Pick a date..."});
        JComboBox<String> timeBox = new JComboBox<>(timeModel);
        timeBox.setPreferredSize(inputSize);
        timeBox.setMaximumSize(inputSize);
        timeBox.setEnabled(false);
        mainContainer.add(timeBox);

        mainContainer.add(Box.createVerticalStrut(30));

        JButton confirmBtn = new JButton("Update Schedule");
        confirmBtn.setPreferredSize(new Dimension(300, 45));
        confirmBtn.setMaximumSize(new Dimension(300, 45));
        confirmBtn.setBackground(SUCCESS); 
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        mainContainer.add(confirmBtn);

        dateChooser.getDateEditor().addPropertyChangeListener("date", evt -> {
            java.util.Date selected = dateChooser.getDate();
            if (selected != null) {
                try {
                    List<String> available = appointmentController.getAvailableSlotsForDate(selected);
                    timeModel.removeAllElements();
                    if (available.isEmpty()) {
                        timeModel.addElement("No slots available");
                        timeBox.setEnabled(false);
                    } else {
                        for (String slot : available) timeModel.addElement(slot);
                        timeBox.setEnabled(true);
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        confirmBtn.addActionListener(e -> {
            if (dateChooser.getDate() == null || !timeBox.isEnabled()) {
                JOptionPane.showMessageDialog(rescheduleDialog, "Please select a valid date and time.");
                return;
            }

            String selectedTime = (String) timeBox.getSelectedItem();
            int actorId = UserSession.getUserId();
            String actorRole = UserSession.getUserRole();

            try {
                if (appointmentController.rescheduleAppointment(appId, dateChooser.getDate(), selectedTime, actorId, actorRole)) {
                    JOptionPane.showMessageDialog(rescheduleDialog, "Appointment Rescheduled.");
                    rescheduleDialog.dispose();
                    invalidateCache();
                    loadUpcomingData(true);
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        rescheduleDialog.add(mainContainer, BorderLayout.CENTER);
        rescheduleDialog.pack();
        rescheduleDialog.setLocationRelativeTo(this);
        rescheduleDialog.setVisible(true);
    }

    private void autoAddLeftLabel(JPanel container, String text, Font font) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(300, 25));
        JLabel label = new JLabel(text);
        label.setFont(font);
        wrapper.add(label);
        container.add(wrapper);
    }
    
    private void handleStaffCancellation(int appId) {
        int confirm = JOptionPane.showConfirmDialog(
            this, 
            "Cancel this appointment? This frees up the slot and notifies the patient.", 
            "Confirm Cancellation", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int actorId = UserSession.getUserId();
                String actorRole = UserSession.getUserRole();
                if (appointmentController.updateAppointmentStatus(appId, "Cancelled", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment Cancelled.");
                    invalidateCache();
                    loadUpcomingData(true); 
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    // ==========================================================
    // Button Renderer and Editor for Action Column
    // ==========================================================

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(WARNING);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setFocusPainted(false);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "Send Reminder" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(WARNING);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            label = (value == null) ? "Send Reminder" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                try {
                    int appId = (int) model.getValueAt(currentRow, 0);
                    int pId = (int) model.getValueAt(currentRow, 1);
                    
                    Patient patient = patientController.getPatientById(pId);
                    List<Appointment> history = appointmentController.getPatientAppointmentHistory(pId);
                    Appointment appointment = history.stream()
                        .filter(a -> a.getAppointmentId() == appId)
                        .findFirst().orElse(null);
                    
                    if (appointment != null && patient != null) {
                        sendManualReminder(appointment, patient);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
                }
            }
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
