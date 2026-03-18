package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JFrame {

    private JPanel sidebar;
    private JButton accessControlBtn, sysLogsBtn, auditBtn, logoutBtn;
    private JPanel subMenuPanel;
    private boolean isOpen = false;

    // Fixed Y positions for buttons to keep absolute positioning consistent
    private final int LOGS_Y = 285;
    private final int AUDIT_Y = 335;
    private final int LOGOUT_Y = 600;

    public AdminDashboard() {
        setTitle("Dental Clinic - Administrator Dashboard");
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

        // Sidebar Header (Logo) - Added "Admin Panel" per your request
        JLabel logoLabel = new JLabel("Admin Panel");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- FIXED BUTTONS ---
        sidebar.add(createSidebarButton("My Dashboard", 100));
        sidebar.add(createSidebarButton("User Activity Logs", 150));

        // --- DROPDOWN BUTTON ---
        accessControlBtn = createSidebarButton("Access Control  ⌄", 230);
        sidebar.add(accessControlBtn);

        // --- SUB-MENU PANEL (Nested modern style) ---
        subMenuPanel = new JPanel();
        subMenuPanel.setLayout(null);
        subMenuPanel.setBackground(new Color(34, 49, 63)); 
        subMenuPanel.setBounds(20, 275, 210, 80); 
        subMenuPanel.setVisible(false);

        subMenuPanel.add(createSubButton("Manage Users", 5));
        subMenuPanel.add(createSubButton("Roles & Permissions", 40));
        sidebar.add(subMenuPanel);

        // --- BUTTONS THAT MOVE ---
        sysLogsBtn = createSidebarButton("System Logs", LOGS_Y);
        auditBtn = createSidebarButton("Audit Trails", AUDIT_Y);
        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(41, 128, 185));

        sidebar.add(sysLogsBtn);
        sidebar.add(auditBtn);
        sidebar.add(logoutBtn);

        // --- ACTION LOGIC ---
        accessControlBtn.addActionListener(e -> toggleMenu());
        
        logoutBtn.addActionListener(e -> {
            new LoginPage();
            dispose();
        });

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- CONTENT AREA ---
        JPanel contentArea = new JPanel(new GridBagLayout());
        contentArea.setBackground(new Color(236, 240, 241));
        
        // Introductory Message Panel
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setBackground(new Color(236, 240, 241));

        JLabel welcomeMsg = new JLabel("Welcome to Your Dashboard");
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));
        welcomeMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subMsg = new JLabel("Select an option from the sidebar to get started");
        subMsg.setFont(new Font("Arial", Font.PLAIN, 18));
        subMsg.setForeground(Color.GRAY);
        subMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        welcomePanel.add(welcomeMsg);
        welcomePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        welcomePanel.add(subMsg);

        contentArea.add(welcomePanel);
        mainPanel.add(contentArea, BorderLayout.CENTER);

        setVisible(true);
    }

    private void toggleMenu() {
        isOpen = !isOpen;
        subMenuPanel.setVisible(isOpen);
        
        // Shift logic: If open, push lower buttons down by 85px
        int shift = isOpen ? 85 : 0;
        
        accessControlBtn.setText(isOpen ? "Access Control  ⌃" : "Access Control  ⌄");
        sysLogsBtn.setLocation(sysLogsBtn.getX(), LOGS_Y + shift);
        auditBtn.setLocation(auditBtn.getX(), AUDIT_Y + shift);
        
        sidebar.repaint();
    }

    private JButton createSidebarButton(String text, int y) {
        JButton btn = new JButton(text);
        btn.setBounds(20, y, 210, 40);
        btn.setBackground(new Color(52, 152, 219));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.PLAIN, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
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