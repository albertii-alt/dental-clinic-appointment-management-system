package com.dentalclinic.view;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.view.staff.*;
import com.dentalclinic.view.components.LogoutDialog;
import com.dentalclinic.view.components.Sidebar;
import com.dentalclinic.view.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javax.swing.Timer;
import com.dentalclinic.view.components.WelcomePanel;

public class StaffDashboard extends JFrame {

    private JPanel mainPanel;
    private JPanel currentContent;
    private Sidebar sidebar;
    private Timer sessionCheckTimer;
    
    private int staffId;
    private String staffName;
    private String username;
    private String email;
    private String role = "Staff";

    public StaffDashboard(int staffId, String staffName, String user, String mail) {
        
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
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/com/dentalclinic/resources/VantageLogo.png");
            if (iconStream != null) {
                setIconImage(javax.imageio.ImageIO.read(iconStream));
            }
        } catch (Exception ignored) {}
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(236, 240, 241));
        
        mainPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });
        
        sidebar = new Sidebar();
        sidebar.addLogo("Staff Portal", staffName, "Staff", () -> {
            switchPanel(new WelcomePanel(staffName, "Select an appointment category to manage the clinic flow"));
            sidebar.clearActiveButton();
            UserSession.updateActivity();
        });
        // Appointment Management Section
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            sidebar.addButton("Pending Appointments", FontAwesomeSolid.CLOCK, () -> {
                switchPanel(new PendingRequestsPanel());
                UserSession.updateActivity();
            });
            
            sidebar.addButton("Today's Appointments", FontAwesomeSolid.CALENDAR_DAY, () -> {
                switchPanel(new TodaysAppointmentsPanel());
                UserSession.updateActivity();
            });
            
            sidebar.addButton("Cancelled Appointments", FontAwesomeSolid.BAN, () -> {
                switchPanel(new CancelledAppointmentsPanel(staffId, staffName));
                UserSession.updateActivity();
            });
            
            sidebar.addButton("Upcoming Appointments", FontAwesomeSolid.CALENDAR_WEEK, () -> {
                switchPanel(new UpcomingAppointmentsPanel());
                UserSession.updateActivity();
            });
        }
        
        // Management Section Label
        sidebar.addLabel("Management", 310);
        
        int yOffset = 340;
        
        // Register Patient
        if (UserSession.hasPermission("MANAGE_PATIENTS")) {
            sidebar.addButtonAt("Register Patient", FontAwesomeSolid.USER_PLUS, yOffset, () -> {
                switchPanel(new RegisterPatientPanel());
                UserSession.updateActivity();
            });
            yOffset += 50;
        }
        
        // Create Appointment
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            sidebar.addButtonAt("Create Appointment", FontAwesomeSolid.CALENDAR_PLUS, yOffset, () -> {
                switchPanel(new StaffBookAppointmentPanel());
                UserSession.updateActivity();
            });
            yOffset += 50;
        }
        
        // View Patient History
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            sidebar.addButtonAt("View Patient History", FontAwesomeSolid.NOTES_MEDICAL, yOffset, () -> {
                switchPanel(new PatientHistoryPanel(false));
                UserSession.updateActivity();
            });
            yOffset += 50;
        }
        
        // Manage Schedule
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            sidebar.addButtonAt("Manage Schedule", FontAwesomeSolid.COG, yOffset, () -> {
                switchPanel(new StaffManageSchedulePanel(staffId, staffName, role));
                UserSession.updateActivity();
            });
        }
        
        // My Settings
        sidebar.addButtonAt("My Settings", FontAwesomeSolid.SLIDERS_H, 540, () -> {
            switchPanel(new com.dentalclinic.view.admin.AccountSettingsPanel(
                staffId, "STAFF", staffName, username, email
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
        switchPanel(new WelcomePanel(staffName, "Select an appointment category to manage the clinic flow"));
        
        setVisible(true);
    }

    private void startSessionMonitor() {
        sessionCheckTimer = new Timer(10000, e -> {
            if (!UserSession.isSessionValid()) {
                sessionCheckTimer.stop();
                com.dentalclinic.view.components.ErrorDialog.show(this, 
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
}
