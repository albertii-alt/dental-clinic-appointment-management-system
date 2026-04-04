package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.*;
import com.dental.clinic.ui.components.LogoutDialog;
import com.dentalclinic.ui.components.Sidebar;
import com.dentalclinic.ui.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class DentistDashboard extends JFrame {

    private JPanel mainPanel;
    private JPanel currentContent;
    private Sidebar sidebar;
    private Timer sessionCheckTimer;
    private JPanel appointmentsSubMenu;
    private SidebarButton viewAppBtn;
    private SidebarButton historyBtn;
    private SidebarButton blockBtn;
    private List<JComponent> componentsToShift = new ArrayList<>();
    private boolean isAppMenuOpen = false;
    private int shiftAmount = 85;
    
    private int staffId;
    private String staffName;
    private String username;
    private String email;
    private String role = "Dentist";

    public DentistDashboard(int staffId, String staffName, String user, String mail) {
        
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
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(236, 240, 241));
        
        // Track activity
        mainPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });
        
        // Create sidebar
        sidebar = new Sidebar();
        sidebar.addLogo("Dentist Portal", () -> {
            showWelcomeScreen();
            sidebar.clearActiveButton();
            UserSession.updateActivity();
        });
        
        // View Appointments Dropdown
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            viewAppBtn = new SidebarButton("View Appointments  ⌄");
            viewAppBtn.setBounds(20, 100, 210, 40);
            viewAppBtn.addActionListener(e -> toggleAppMenu());
            sidebar.add(viewAppBtn);
            
            appointmentsSubMenu = sidebar.createSubMenu(145, 80);
            
            sidebar.addSubButton(appointmentsSubMenu, "Today's Schedule", 5, () -> {
                switchPanel(new TodaysAppointmentsPanel());
                UserSession.updateActivity();
            });
            
            sidebar.addSubButton(appointmentsSubMenu, "Upcoming Treatments", 40, () -> {
                switchPanel(new UpcomingAppointmentsPanel());
                UserSession.updateActivity();
            });
        }
        
        // View Patient History
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            historyBtn = sidebar.addButtonAt("View Patient History", 150, () -> {
                switchPanel(new PatientHistoryPanel(true));
                UserSession.updateActivity();
            });
        }
        
        // Block Time Slots
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            blockBtn = sidebar.addButtonAt("Block Time Slots", 200, () -> {
                switchPanel(new StaffManageSchedulePanel(staffId, staffName, role));
                UserSession.updateActivity();
            });
        }
        
        // Register components that need to shift when submenu opens
        if (historyBtn != null) {
            componentsToShift.add(historyBtn);
        }
        if (blockBtn != null) {
            componentsToShift.add(blockBtn);
        }
        
        // My Account Settings
        sidebar.addButtonAt("My Account Settings", 550, () -> {
            switchPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                staffId, role, staffName, username, email
            ));
            UserSession.updateActivity();
        });
        
        // Logout
        sidebar.addSpecialButton("Logout", 600, new Color(192, 57, 43), () -> {
            boolean confirm = LogoutDialog.show(this);
            if (confirm) {
                if (sessionCheckTimer != null) sessionCheckTimer.stop();
                UserSession.logout();
                new LoginPage();
                dispose();
            }
        });
        
        add(sidebar, BorderLayout.WEST);
        add(mainPanel, BorderLayout.CENTER);
        
        startSessionMonitor();
        showWelcomeScreen();
        
        // Auto-send reminders
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
        UserSession.updateActivity();
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
        
        int shift = isAppMenuOpen ? shiftAmount : -shiftAmount;
        
        for (JComponent comp : componentsToShift) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        sidebar.repaint();
        UserSession.updateActivity();
    }
}