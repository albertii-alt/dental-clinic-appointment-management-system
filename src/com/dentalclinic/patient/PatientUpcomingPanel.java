package com.dentalclinic.patient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.dao.AppointmentDAO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class PatientUpcomingPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private List<Appointment> upcomingList;
    private List<Appointment> filteredList = new ArrayList<>();
    private com.dentalclinic.service.AppointmentService appService = new com.dentalclinic.service.AppointmentService();

    public PatientUpcomingPanel(int patientID) {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(236, 240, 241));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("My Upcoming Appointments");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        String[] columns = {"Service", "Date", "Time", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        
        // Add a nice "Double Click for Receipt" message
        JLabel hint = new JLabel("Double-click an appointment to view details or download receipt.");
        hint.setFont(new Font("Arial", Font.ITALIC, 12));
        add(hint, BorderLayout.SOUTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        // Reuse your existing detail logic from ViewAppointmentsPanel
                        showApprovedAppointmentDetails(row, patientID);
                    }
                }
            }
        });

        loadData(patientID);
    }

    private void loadData(int pID) {
        try {
            model.setRowCount(0);
            AppointmentDAO dao = new AppointmentDAO();
            upcomingList = dao.getUpcomingScheduleByPatient(pID);

            // --- ADD THIS LINE ---
            // This syncs the list used by the double-click logic with the data from the DB
            filteredList = new ArrayList<>(upcomingList); 

            if (upcomingList.isEmpty()) {   
                setLayout(new GridBagLayout());
                removeAll();
                JLabel noApp = new JLabel("You have no upcoming appointments for today.");
                noApp.setFont(new Font("Arial", Font.BOLD, 18));
                noApp.setForeground(Color.GRAY);
                add(noApp);
            } else {
                for (Appointment a : upcomingList) {
                    model.addRow(new Object[]{
                        a.getServiceType(),
                        a.getAppointmentDate(),
                        a.getAppointmentTime(),
                        "Confirmed"
                    });
                }
            }
            // Refresh the UI to show the table
            revalidate();
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showApprovedAppointmentDetails(int rowIndex, int pID) {
        try {
            // --- 1. DATA FETCHING ---
            // Using the same list logic you provided
            Appointment app = filteredList.get(rowIndex);
            com.dentalclinic.dao.PatientDAO pDao = new com.dentalclinic.dao.PatientDAO();
            com.dentalclinic.model.Patient p = pDao.getPatientById(pID);

            // --- 2. UI PANEL SETUP (Identical to your layout) ---
            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(Color.WHITE);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            Font headerFont = new Font("Arial", Font.BOLD, 16);
            Font dataFont = new Font("Arial", Font.PLAIN, 14);

            // APPOINTMENT SUMMARY SECTION
            JLabel title1 = new JLabel("APPOINTMENT SUMMARY");
            title1.setFont(new Font("Arial", Font.BOLD, 14));
            detailPanel.add(title1);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createDetailLabel("Service Type:", app.getServiceType(), dataFont));
            detailPanel.add(createDetailLabel("Date:", app.getAppointmentDate().toString(), dataFont));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime(), dataFont));

            // Status specifically set to Approved styling as per your color logic
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            statusLbl.setForeground(new Color(46, 204, 113)); // Your Approved Green
            statusLbl.setFont(headerFont);
            detailPanel.add(statusLbl);
            detailPanel.add(Box.createVerticalStrut(15));

            // PATIENT INFORMATION SECTION
            JLabel title2 = new JLabel("PATIENT INFORMATION");
            title2.setFont(new Font("Arial", Font.BOLD, 14));
            detailPanel.add(title2);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            String fullName = p.getFirstName() + " " + (p.getMiddleName().isEmpty() ? "" : p.getMiddleName() + " ") + p.getLastName();
            detailPanel.add(createDetailLabel("Full Name:", fullName, dataFont));
            detailPanel.add(createDetailLabel("Birthdate:", p.getBirthDate().toString(), dataFont));
            detailPanel.add(createDetailLabel("Age at Booking:", String.valueOf(app.getAgeAtVisit()), dataFont));
            detailPanel.add(createDetailLabel("Contact No:", app.getContactAtVisit(), dataFont));
            detailPanel.add(createDetailLabel("Full Address:", "<html><p style='width:250px'>" + p.getAddress() + "</p></html>", dataFont));

            // --- 3. SEPARATED BUTTON LOGIC (Approved Only) ---
            java.util.List<String> optionsList = new java.util.ArrayList<>();

            // We only add Download Receipt because this method is strictly for Approved
            optionsList.add("Download Receipt");
            optionsList.add("Close");

            String[] options = optionsList.toArray(new String[0]);

            detailPanel.setSize(new Dimension(400, 450));
            layoutComponent(detailPanel);

            int selection = JOptionPane.showOptionDialog(
                this, detailPanel, "Appointment Summary", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[options.length - 1]
            );

            // --- 4. SELECTION LOGIC ---
            if (selection != -1) {
                String selectedValue = options[selection];

                if (selectedValue.equals("Download Receipt")) {
                    savePanelAsImage(detailPanel, "Receipt_" + app.getAppointmentId());
                }
                // "Close" does nothing and simply dismisses the dialog
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
        private JLabel createDetailLabel(String title, String value, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> " + value + "</html>");
        label.setFont(font);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return label;
    }

    // Helper to force layout so image capture isn't blank/messed up
    private void layoutComponent(Component c) {
        synchronized (c.getTreeLock()) {
            c.doLayout();
            if (c instanceof Container) {
                for (Component child : ((Container) c).getComponents()) {
                    layoutComponent(child);
                }
            }
        }
    }
    
    private void savePanelAsImage(JPanel panel, String filename) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        panel.printAll(g2d);
        g2d.dispose();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filename + ".png"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(image, "png", fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Receipt saved successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
       private void handleCancellation(Appointment app, int pID) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to cancel this appointment request?", 
            "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // 1. Get the patient's session info for the log
                // Since this is the Patient's panel, the actor is the Patient
                int actorId = com.dentalclinic.util.UserSession.getUserId();
                String actorRole = com.dentalclinic.util.UserSession.getUserRole();

                // 2. Call the overloaded method to update status AND log the action
                if (appService.updateAppointmentStatus(app.getAppointmentId(), "Cancelled", actorId, actorRole)) {
                    JOptionPane.showMessageDialog(this, "Appointment Cancelled Successfully.");
                    loadData(pID); // Refresh the table
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}