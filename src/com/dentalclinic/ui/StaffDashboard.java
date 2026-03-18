package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;

public class StaffDashboard extends JFrame {

    private JPanel sidebar;
    private JButton logoutBtn;

    public StaffDashboard() {
        setTitle("Dental Clinic - Staff Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // --- SIDEBAR PANEL ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80)); // Matching Admin Dark Blue
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Staff Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- BUTTONS BASED ON USE CASE DIAGRAM ---
        sidebar.add(createSidebarButton("Today's Appointments", 100));
        sidebar.add(createSidebarButton("Pending Appointments", 150));
        sidebar.add(createSidebarButton("Cancelled Appointments", 200));
        sidebar.add(createSidebarButton("Upcoming Appointments", 250));
        
        // Separator label for clarity
        JLabel patientLabel = new JLabel("Management");
        patientLabel.setForeground(new Color(171, 183, 183));
        patientLabel.setBounds(25, 305, 150, 20);
        sidebar.add(patientLabel);

        sidebar.add(createSidebarButton("Register Patient", 330));
        sidebar.add(createSidebarButton("Create Appointment", 380));
        sidebar.add(createSidebarButton("View Patient History", 430));
        sidebar.add(createSidebarButton("Manage Schedule", 480));

        // Logout Button at fixed bottom
        logoutBtn = createSidebarButton("Logout", 600);
        logoutBtn.setBackground(new Color(41, 128, 185)); // Lighter Blue matching Admin
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- CONTENT AREA ---
        JPanel contentArea = new JPanel(new GridBagLayout());
        contentArea.setBackground(new Color(236, 240, 241));
        
        JLabel welcomeMsg = new JLabel("Welcome, Staff Member");
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));
        
        JLabel subMsg = new JLabel("Select an appointment category to manage the clinic flow");
        subMsg.setFont(new Font("Arial", Font.PLAIN, 18));
        subMsg.setForeground(Color.GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        contentArea.add(welcomeMsg, gbc);
        gbc.gridy = 1;
        contentArea.add(subMsg, gbc);

        mainPanel.add(contentArea, BorderLayout.CENTER);

        // --- LOGOUT ACTION ---
        logoutBtn.addActionListener(e -> {
            new LoginPage();
            dispose();
        });

        setVisible(true);
    }

    // Matching the exact helper method from Admin for consistency
    private JButton createSidebarButton(String text, int yPos) {
        JButton button = new JButton(text);
        button.setBounds(20, yPos, 210, 40);
        button.setFocusPainted(false);
        button.setBackground(new Color(52, 152, 219)); // Same Blue
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 13)); // Slightly smaller font to fit longer text
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
}