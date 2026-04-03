package com.dentalclinic.ui;

import com.dentalclinic.staff.CancelledAppointmentsPanel;
import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.PendingRequestsPanel;
import com.dentalclinic.staff.TodaysAppointmentsPanel;
import com.dentalclinic.staff.UpcomingAppointmentsPanel;
import com.dental.clinic.ui.components.LogoutDialog;
import com.dentalclinic.util.UserSession;
import javax.swing.Timer;

public class StaffDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainPanel;
    private JPanel currentContent;
    private JButton logoutBtn;
    private int staffId;
    private String staffName;
    private String username;
    private String email;
    private String role = "Staff";
    
    private Timer sessionCheckTimer; // Timer for session monitoring
    
    private final int LOGOUT_Y = 600;

    public StaffDashboard(int staffId, String staffName, String user, String mail) {
        
        // Check session validity before proceeding
        if (!UserSession.isSessionValid()) {
            new LoginPage();
            dispose();
            return;
        }
        
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
        
        // Track activity on main panel
        mainPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });

        // --- SIDEBAR PANEL ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Staff Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        logoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showWelcomeScreen();
                UserSession.updateActivity();
            }
        });
        sidebar.add(logoLabel);

        // --- CREATE BUTTONS AND ADD ACTIONS ---
        
        // 1. Pending Appointments (Permission: MANAGE_APPOINTMENTS)
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            JButton pendingBtn = createSidebarButton("Pending Appointments", 150);
            pendingBtn.addActionListener(e -> {
                switchPanel(new PendingRequestsPanel());
                UserSession.updateActivity();
            });
            sidebar.add(pendingBtn);

            JButton todayBtn = createSidebarButton("Today's Appointments", 100);
            todayBtn.addActionListener(e -> {
                switchPanel(new TodaysAppointmentsPanel());
                UserSession.updateActivity();
            });
            sidebar.add(todayBtn);
            
            JButton btnCancelled = createSidebarButton("Cancelled Appointments", 200);
            btnCancelled.addActionListener(e -> {
                switchPanel(new CancelledAppointmentsPanel(this.staffId, this.staffName));
                UserSession.updateActivity();
            }); 
            sidebar.add(btnCancelled);

            JButton upcomingBtn = createSidebarButton("Upcoming Appointments", 250);
            upcomingBtn.addActionListener(e -> {
                switchPanel(new UpcomingAppointmentsPanel());
                UserSession.updateActivity();
            });
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
            regBtn.addActionListener(e -> {
                switchPanel(new com.dentalclinic.staff.RegisterPatientPanel());
                UserSession.updateActivity();
            });
            sidebar.add(regBtn);
        }
        
        // Permission: MANAGE_APPOINTMENTS (Used for manual creation)
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            JButton createBtn = createSidebarButton("Create Appointment", 390);
            createBtn.addActionListener(e -> {
                switchPanel(new com.dentalclinic.staff.StaffBookAppointmentPanel());
                UserSession.updateActivity();
            });
            sidebar.add(createBtn);
        }
        
        // Permission: VIEW_MEDICAL_HISTORY
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            JButton historyBtn = createSidebarButton("View Patient History", 440);
            historyBtn.addActionListener(e -> {
                switchPanel(new com.dentalclinic.staff.PatientHistoryPanel(false));
                UserSession.updateActivity();
            });
            sidebar.add(historyBtn);
        }
        
        // Permission: MANAGE_SCHEDULE
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            JButton manageSchedBtn = createSidebarButton("Manage Schedule", 490);
            manageSchedBtn.addActionListener(e -> {
                switchPanel(new com.dentalclinic.staff.StaffManageSchedulePanel(staffId, staffName, role));
                UserSession.updateActivity();
            });
            sidebar.add(manageSchedBtn);
        }
        
        // Settings and Logout usually don't need specific permissions as they are basic user functions
        JButton settingsBtn = createSidebarButton("My Settings", 540);
        settingsBtn.addActionListener(e -> {
            switchPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                this.staffId, "STAFF", this.staffName, this.username, this.email
            ));
            UserSession.updateActivity();
        });
        sidebar.add(settingsBtn);
        
        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.addActionListener(e -> {
            boolean confirm = LogoutDialog.show(this);
            if (confirm) {
                // Stop session timer
                if (sessionCheckTimer != null) {
                    sessionCheckTimer.stop();
                }
                // Clear session
                UserSession.logout();
                // Return to login
                new LoginPage();
                dispose();
            }
        });
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // Start session monitor
        startSessionMonitor();
        
        // --- INITIAL WELCOME CONTENT ---
        showWelcomeScreen();
        
                // Auto-send reminders for tomorrow's appointments (runs in background)
        new Thread(() -> {
            try {
                com.dentalclinic.service.AppointmentService appService = new com.dentalclinic.service.AppointmentService();
                int sent = appService.sendAllRemindersForTomorrow();
                if (sent > 0) {
                    System.out.println("Sent " + sent + " appointment reminders for tomorrow");
                }
            } catch (Exception e) {
                System.err.println("Failed to send reminders: " + e.getMessage());
            }
        }).start();

        setVisible(true);
        
    }

    private void startSessionMonitor() {
        sessionCheckTimer = new Timer(10000, e -> {
            if (!UserSession.isSessionValid()) {
                sessionCheckTimer.stop();
                com.dental.clinic.ui.components.ErrorDialog.show(this, 
                    "Session Expired", 
                    "Your session has expired due to inactivity.\nPlease login again.");
                UserSession.logout();
                new LoginPage();
                dispose();
            }
        });
        sessionCheckTimer.start();
    }

    // HELPER METHOD TO SWITCH PANELS
    private void switchPanel(JPanel newPanel) {
        if (currentContent != null) {
            mainPanel.remove(currentContent);
        }
        currentContent = newPanel;
        mainPanel.add(currentContent, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        UserSession.updateActivity(); // Track activity on panel change
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
        gbc.gridx = 0; 
        gbc.gridy = 0;
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
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });
        
        return button;
    }
    
    private void showPanel(JPanel panel) {
        mainPanel.removeAll();
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
        UserSession.updateActivity();
    }
}