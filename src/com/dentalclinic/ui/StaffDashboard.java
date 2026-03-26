package com.dentalclinic.ui;

import com.dentalclinic.staff.CancelledAppointmentsPanel;
import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.PendingRequestsPanel; // Import your panel
import com.dentalclinic.staff.TodaysAppointmentsPanel;
import com.dentalclinic.staff.UpcomingAppointmentsPanel;
import com.dentalclinic.util.UserSession;

public class StaffDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainPanel; // Moved to class level
    private JPanel currentContent; // To track what's currently in the center
    private JButton logoutBtn;
    private int staffId;
    private String staffName;
    private String username;
    private String email;
    private String role = "Staff";

    public StaffDashboard(int staffId, String staffName, String user, String mail) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.username = user;
        this.email = mail;
        setTitle("Dental Clinic - Staff Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // --- SIDEBAR PANEL ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Staff Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- CREATE BUTTONS AND ADD ACTIONS ---
        
        // --- CREATE BUTTONS AND ADD ACTIONS ---
        
        // 1. Pending Appointments (Permission: MANAGE_APPOINTMENTS)
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            JButton pendingBtn = createSidebarButton("Pending Appointments", 150);
            pendingBtn.addActionListener(e -> switchPanel(new PendingRequestsPanel()));
            sidebar.add(pendingBtn);

            JButton todayBtn = createSidebarButton("Today's Appointments", 100);
            todayBtn.addActionListener(e -> switchPanel(new TodaysAppointmentsPanel()));
            sidebar.add(todayBtn);
            
            JButton btnCancelled = createSidebarButton("Cancelled Appointments", 200);
            btnCancelled.addActionListener(e -> switchPanel(new CancelledAppointmentsPanel())); 
            sidebar.add(btnCancelled);

            JButton upcomingBtn = createSidebarButton("Upcoming Appointments", 250);
            upcomingBtn.addActionListener(e -> switchPanel(new UpcomingAppointmentsPanel()));
            sidebar.add(upcomingBtn);
        }
        
        // --- MANAGEMENT SECTION ---
        JLabel patientLabel = new JLabel("Management");
        patientLabel.setForeground(new Color(171, 183, 183));
        patientLabel.setBounds(25, 310, 150, 20);
        sidebar.add(patientLabel);

        // Permission: MANAGE_PATIENTS
        if (UserSession.hasPermission("MANAGE_PATIENTS")) {
            JButton regBtn = createSidebarButton("Register Patient", 340);
            regBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.RegisterPatientPanel()));
            sidebar.add(regBtn);
        }
        
        // Permission: MANAGE_APPOINTMENTS (Used for manual creation)
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            JButton createBtn = createSidebarButton("Create Appointment", 390);
            createBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.StaffBookAppointmentPanel()));
            sidebar.add(createBtn);
        }
        
        // Permission: VIEW_MEDICAL_HISTORY
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            JButton historyBtn = createSidebarButton("View Patient History", 440);
            historyBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.PatientHistoryPanel(false)));
            sidebar.add(historyBtn);
        }
        
        // Permission: MANAGE_SCHEDULE
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            JButton manageSchedBtn = createSidebarButton("Manage Schedule", 490);
            manageSchedBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.StaffManageSchedulePanel(staffId, staffName, role)));
            sidebar.add(manageSchedBtn);
        }
        
        // Settings and Logout usually don't need specific permissions as they are basic user functions
        JButton settingsBtn = createSidebarButton("My Settings", 540);
        settingsBtn.addActionListener(e -> {
            switchPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                this.staffId, "STAFF", this.staffName, this.username, this.email
            ));
        });
        sidebar.add(settingsBtn);
        
        logoutBtn = createSidebarButton("Logout", 600);
        logoutBtn.setBackground(new Color(192, 57, 43));
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- INITIAL WELCOME CONTENT ---
        showWelcomeScreen();

        // --- LOGOUT ACTION ---
        logoutBtn.addActionListener(e -> {
             new LoginPage(); 
            dispose();
        });

        setVisible(true);
    }

    // HELPER METHOD TO SWITCH PANELS
    private void switchPanel(JPanel newPanel) {
        if (currentContent != null) {
            mainPanel.remove(currentContent);
        }
        currentContent = newPanel;
        mainPanel.add(currentContent, BorderLayout.CENTER);
        mainPanel.revalidate(); // Refresh layout
        mainPanel.repaint();    // Redraw screen
    }

    private void showWelcomeScreen() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        welcomePanel.setBackground(new Color(236, 240, 241));
        
        JLabel welcomeMsg = new JLabel("Welcome, " + staffName);
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));
        
        JLabel subMsg = new JLabel("Select an appointment category to manage the clinic flow");
        subMsg.setFont(new Font("Arial", Font.PLAIN, 18));
        subMsg.setForeground(Color.GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        welcomePanel.add(welcomeMsg, gbc);
        gbc.gridy = 1;
        welcomePanel.add(subMsg, gbc);

        switchPanel(welcomePanel);
    }

    private JButton createSidebarButton(String text, int yPos) {
        JButton button = new JButton(text);
        button.setBounds(20, yPos, 210, 40);
        button.setFocusPainted(false);
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    private void showPanel(JPanel panel) {
        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }
}