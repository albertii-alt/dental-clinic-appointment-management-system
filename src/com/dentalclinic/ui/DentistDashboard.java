package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.*; // Import panels from the staff package

public class DentistDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainPanel;
    private JPanel currentContent;
    private JButton manageScheduleBtn, logoutBtn, viewAppBtn, historyBtn, blockBtn;
    private JPanel scheduleSubMenu, appointmentsSubMenu;
    private boolean isScheduleOpen = false;
    private boolean isAppMenuOpen = false;

    private final int LOGOUT_Y = 600;

    public DentistDashboard() {
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

        // 1. View Appointments Dropdown
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

        // 2. View Patient History
        historyBtn = createSidebarButton("View Patient History", 150); 
        historyBtn.addActionListener(e -> switchPanel(new PatientHistoryPanel(true))); 
        sidebar.add(historyBtn);

        // 3. Block Time Slots
        blockBtn = createSidebarButton("Block Time Slots", 200);
        blockBtn.addActionListener(e -> switchPanel(new StaffManageSchedulePanel()));
        sidebar.add(blockBtn);

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
        JLabel welcomeMsg = new JLabel("Welcome, Dr. Dentist");
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomePanel.add(welcomeMsg);
        switchPanel(welcomePanel);
    }

    private void toggleAppMenu() {
        isAppMenuOpen = !isAppMenuOpen;
        appointmentsSubMenu.setVisible(isAppMenuOpen);
        viewAppBtn.setText(isAppMenuOpen ? "View Appointments  ⌃" : "View Appointments  ⌄");

        int offset = isAppMenuOpen ? 85 : 0;
        // Shift buttons below the submenu
        historyBtn.setLocation(historyBtn.getX(), 150 + offset);
        blockBtn.setLocation(blockBtn.getX(), 200 + offset);
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