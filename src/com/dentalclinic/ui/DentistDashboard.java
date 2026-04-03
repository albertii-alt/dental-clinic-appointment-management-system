package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.*; // Import panels from the staff package
import com.dental.clinic.ui.components.LogoutDialog;
import com.dentalclinic.util.UserSession;
import javax.swing.Timer;

public class DentistDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainPanel;
    private JPanel currentContent;
    private JButton manageScheduleBtn, logoutBtn, viewAppBtn, historyBtn, blockBtn;
    private JPanel scheduleSubMenu, appointmentsSubMenu;
    private boolean isScheduleOpen = false;
    private boolean isAppMenuOpen = false;
    private int staffId;
    private String staffName;
    private int loggedId;
    private String fullName;
    private String username;
    private String email;
    private String role = "Dentist";
    
    private Timer sessionCheckTimer; // Timer for session monitoring

    private final int LOGOUT_Y = 600;

    public DentistDashboard(int staffId, String staffName, String user, String mail) {
        
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
        
        setTitle("Dental Clinic - Dentist Dashboard");
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

        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80)); 
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Dentist Portal");
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

        // 1. View Appointments Dropdown (Permission: MANAGE_APPOINTMENTS)
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            viewAppBtn = createSidebarButton("View Appointments ⌄", 100);
            viewAppBtn.addActionListener(e -> {
                toggleAppMenu();
                UserSession.updateActivity();
            });
            sidebar.add(viewAppBtn);

            appointmentsSubMenu = new JPanel(null);
            appointmentsSubMenu.setBackground(new Color(34, 49, 63));
            appointmentsSubMenu.setBounds(20, 145, 210, 80);
            appointmentsSubMenu.setVisible(false);

            JButton todayBtn = createSubButton("Today's Schedule", 5);
            JButton upcomingBtn = createSubButton("Upcoming Treatments", 40);

            todayBtn.addActionListener(e -> {
                switchPanel(new TodaysAppointmentsPanel());
                UserSession.updateActivity();
            }); 
            upcomingBtn.addActionListener(e -> {
                switchPanel(new UpcomingAppointmentsPanel());
                UserSession.updateActivity();
            });

            appointmentsSubMenu.add(todayBtn);
            appointmentsSubMenu.add(upcomingBtn);
            sidebar.add(appointmentsSubMenu);
        }

        // 2. View Patient History (Permission: VIEW_MEDICAL_HISTORY)
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            historyBtn = createSidebarButton("View Patient History", 150); 
            historyBtn.addActionListener(e -> {
                switchPanel(new PatientHistoryPanel(true));
                UserSession.updateActivity();
            }); 
            sidebar.add(historyBtn);
        }

        // 3. Block Time Slots (Permission: MANAGE_SCHEDULE)
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            blockBtn = createSidebarButton("Block Time Slots", 200);
            blockBtn.addActionListener(e -> {
                switchPanel(new StaffManageSchedulePanel(staffId, staffName, role));
                UserSession.updateActivity();
            });
            sidebar.add(blockBtn);
        }
        
        // Settings (No permission required, but good to have visible)
        JButton settingsBtn = createSidebarButton("My Account Settings", 550);
        settingsBtn.addActionListener(e -> {
            switchPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                staffId, role, staffName, username, email
            ));
            UserSession.updateActivity();
        });
        sidebar.add(settingsBtn);
        
        // Logout
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
        JLabel welcomeMsg = new JLabel("Welcome, Dr. " + staffName);
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(44, 62, 80));
        welcomePanel.add(welcomeMsg);
        switchPanel(welcomePanel);
    }

    private void toggleAppMenu() {
        if (appointmentsSubMenu == null) return;

        isAppMenuOpen = !isAppMenuOpen;
        appointmentsSubMenu.setVisible(isAppMenuOpen);
        viewAppBtn.setText(isAppMenuOpen ? "View Appointments  ⌃" : "View Appointments  ⌄");

        int offset = isAppMenuOpen ? 85 : 0;
        
        // Shift buttons below the submenu ONLY if they exist
        if (historyBtn != null) {
            historyBtn.setLocation(historyBtn.getX(), 150 + offset);
        }
        if (blockBtn != null) {
            blockBtn.setLocation(blockBtn.getX(), 200 + offset);
        }
        sidebar.repaint();
        UserSession.updateActivity();
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

    private JButton createSubButton(String text, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(10, y, 190, 30);
        btn.setBackground(new Color(34, 49, 63));
        btn.setForeground(new Color(200, 200, 200));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });
        
        return btn;
    }
}