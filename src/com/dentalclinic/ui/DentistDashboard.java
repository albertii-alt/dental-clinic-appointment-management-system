package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.*; // Import panels from the staff package
import com.dentalclinic.util.UserSession;

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

    private final int LOGOUT_Y = 600;

    public DentistDashboard(int staffId, String staffName, String user, String mail) {
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

        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80)); 
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Dentist Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // 1. View Appointments Dropdown (Permission: MANAGE_APPOINTMENTS)
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            viewAppBtn = createSidebarButton("View Appointments ⌄", 100);
            viewAppBtn.addActionListener(e -> toggleAppMenu());
            sidebar.add(viewAppBtn);

            appointmentsSubMenu = new JPanel(null);
            appointmentsSubMenu.setBackground(new Color(34, 49, 63));
            appointmentsSubMenu.setBounds(20, 145, 210, 80);
            appointmentsSubMenu.setVisible(false);

            JButton todayBtn = createSubButton("Today's Schedule", 5);
            JButton upcomingBtn = createSubButton("Upcoming Treatments", 40);

            todayBtn.addActionListener(e -> switchPanel(new TodaysAppointmentsPanel())); 
            upcomingBtn.addActionListener(e -> switchPanel(new UpcomingAppointmentsPanel()));

            appointmentsSubMenu.add(todayBtn);
            appointmentsSubMenu.add(upcomingBtn);
            sidebar.add(appointmentsSubMenu);
        }

        // 2. View Patient History (Permission: VIEW_MEDICAL_HISTORY)
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            historyBtn = createSidebarButton("View Patient History", 150); 
            historyBtn.addActionListener(e -> switchPanel(new PatientHistoryPanel(true))); 
            sidebar.add(historyBtn);
        }

        // 3. Block Time Slots (Permission: MANAGE_SCHEDULE)
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            blockBtn = createSidebarButton("Block Time Slots", 200);
            blockBtn.addActionListener(e -> switchPanel(new StaffManageSchedulePanel(staffId, staffName, role)));
            sidebar.add(blockBtn);
        }
        
        // Settings (No permission required, but good to have visible)
        JButton settingsBtn = createSidebarButton("My Account Settings", 550);
        settingsBtn.addActionListener(e -> {
            switchPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                staffId, role, staffName, username, email
            ));
        });
        sidebar.add(settingsBtn);
        
        // Logout
        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.addActionListener(e -> { new LoginPage(); dispose(); });
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);
        showWelcomeScreen();
        setVisible(true);
    }

    private void switchPanel(JPanel newPanel) {
        if (currentContent != null) mainPanel.remove(currentContent);
        currentContent = newPanel;
        mainPanel.add(currentContent, BorderLayout.CENTER);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void showWelcomeScreen() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        JLabel welcomeMsg = new JLabel("Welcome, Dr. " + staffName); // Use the variable
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomePanel.add(welcomeMsg);
        switchPanel(welcomePanel);
    }

    private void toggleAppMenu() {
        if (appointmentsSubMenu == null) return; // Safety check

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
        return btn;
    }
}