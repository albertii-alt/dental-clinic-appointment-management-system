package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;

public class DentistDashboard extends JFrame {

    private JPanel sidebar;
    private JButton manageScheduleBtn, logoutBtn;
    private JPanel scheduleSubMenu;
    private boolean isScheduleOpen = false;

    // Base Y positions for elements below the dropdown
    private final int LOGOUT_Y = 600;

    public DentistDashboard() {
        setTitle("Dental Clinic - Dentist Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // --- SIDEBAR PANEL ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80)); 
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Dentist Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- BUTTONS BASED ON USE CASE ---
        sidebar.add(createSidebarButton("Today's Schedule", 100));
        sidebar.add(createSidebarButton("Upcoming Treatments", 150));
        sidebar.add(createSidebarButton("View Appointments", 200));
        sidebar.add(createSidebarButton("Update Treatment Record", 250));
        sidebar.add(createSidebarButton("View Patient History", 300));

        // --- MANAGE SCHEDULE DROPDOWN (Accordion Style) ---
        manageScheduleBtn = createSidebarButton("Manage Schedule  ⌄", 350);
        sidebar.add(manageScheduleBtn);

        // Sub-menu Panel
        scheduleSubMenu = new JPanel();
        scheduleSubMenu.setLayout(null);
        scheduleSubMenu.setBackground(new Color(34, 49, 63)); // Darker inset background
        scheduleSubMenu.setBounds(20, 395, 210, 80); // Height fits 2 sub-buttons
        scheduleSubMenu.setVisible(false);

        scheduleSubMenu.add(createSubButton("Set Working Hours", 5));
        scheduleSubMenu.add(createSubButton("Block Time Slots", 40));
        sidebar.add(scheduleSubMenu);

        // --- LOGOUT (The only button below the dropdown in this case) ---
        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(41, 128, 185));
        sidebar.add(logoutBtn);

        // Dropdown Toggle Logic
        manageScheduleBtn.addActionListener(e -> toggleScheduleMenu());

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- CONTENT AREA ---
        JPanel contentArea = new JPanel(new GridBagLayout());
        contentArea.setBackground(new Color(236, 240, 241));
        
        JLabel welcomeMsg = new JLabel("Welcome, Dr. Dentist");
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));
        
        contentArea.add(welcomeMsg);
        mainPanel.add(contentArea, BorderLayout.CENTER);

        logoutBtn.addActionListener(e -> { new LoginPage(); dispose(); });
        
        setVisible(true);
    }

    private void toggleScheduleMenu() {
        isScheduleOpen = !isScheduleOpen;
        scheduleSubMenu.setVisible(isScheduleOpen);
        manageScheduleBtn.setText(isScheduleOpen ? "Manage Schedule  ⌃" : "Manage Schedule  ⌄");
        
        // Note: In this specific layout, only the logout is below. 
        // If you add more buttons between Manage Schedule and Logout later, 
        // you would shift them here using .setLocation()
        sidebar.repaint();
    }

    // Standard Sidebar Button Style
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

    // Inset Sub-menu Button Style
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