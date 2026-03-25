package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.admin.AdminDashboardPanel;
import com.dentalclinic.admin.ClinicSettingsPanel;
import com.dentalclinic.admin.ManageUsersPanel;


public class AdminDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainContent; // The dedicated area for our panels
    private JButton accessControlBtn, sysLogsBtn, auditBtn, logoutBtn;
    private JPanel subMenuPanel;
    private boolean isOpen = false;
    private JButton clinicConfigBtn;
    
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String currentAdminName;  // Add this
    private String currentAdminEmail; // Add this
    private String currentAdminUsername;
    private ManageUsersPanel manageUsersPanel;
    private AdminDashboardPanel dashboardStatsPanel;
    
    // Y-Coordinates for Sidebar Buttons
    private final int CONFIG_Y = 200; 
    private final int ACCESS_Y = 250; 
    private final int LOGS_Y = 305;   
    private final int AUDIT_Y = 355;  
    private final int LOGOUT_Y = 600;

    public AdminDashboard(int loggedUserId, boolean isSuper, String fullName, String email, String username) {
        this.currentAdminId = loggedUserId;
        this.isSuperAdmin = isSuper;
        this.currentAdminName = fullName; 
        this.currentAdminEmail = email; 
        this.currentAdminUsername = username;
        
        // Initialize the panel now that we have the ID
        this.manageUsersPanel = new ManageUsersPanel(this.currentAdminId, this.isSuperAdmin);
        this.dashboardStatsPanel = new AdminDashboardPanel();

        setTitle("Dental Clinic - Administrator Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- MAIN LAYOUT ---
        setLayout(new BorderLayout());

        // --- SIDEBAR ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Admin Panel");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- SIDEBAR BUTTONS ---
        JButton myDashBtn = createSidebarButton("My Dashboard", 100);
        myDashBtn.addActionListener(e -> {
            showPanel(dashboardStatsPanel);
            dashboardStatsPanel.refreshStats(); // Ensure numbers are fresh
        });
        sidebar.add(myDashBtn);
        
        JButton logsBtn = createSidebarButton("Audit Trails", 150);
        sidebar.add(logsBtn);
        // Pass the session data here
        logsBtn.addActionListener(e -> showPanel(new com.dentalclinic.admin.AuditTrailsPanel(currentAdminId, isSuperAdmin)));
        
        clinicConfigBtn = createSidebarButton("Clinic Configuration", CONFIG_Y);
        sidebar.add(clinicConfigBtn);
        clinicConfigBtn.addActionListener(e -> showPanel(new ClinicSettingsPanel(currentAdminId, isSuperAdmin)));

        accessControlBtn = createSidebarButton("Access Control  ⌄", ACCESS_Y);
        sidebar.add(accessControlBtn);
        accessControlBtn.addActionListener(e -> toggleMenu());

        // Sub-Menu
        subMenuPanel = new JPanel();
        subMenuPanel.setLayout(null);
        subMenuPanel.setBackground(new Color(34, 49, 63)); 
        subMenuPanel.setBounds(20, 295, 210, 80); 
        subMenuPanel.setVisible(false);

        JButton manageUsersBtn = createSubButton("Manage Users", 5);
        manageUsersBtn.addActionListener(e -> {
            showPanel(manageUsersPanel);
            manageUsersPanel.refreshTable();
        });
        
        JButton rolesBtn = createSubButton("Roles & Permissions", 40);
        rolesBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Coming soon!"));

        subMenuPanel.add(manageUsersBtn);
        subMenuPanel.add(rolesBtn);
        sidebar.add(subMenuPanel);

        // Lower Buttons
        sysLogsBtn = createSidebarButton("System Logs", LOGS_Y);
        // Add this action listener to make the button work
        sysLogsBtn.addActionListener(e -> {
            // 1. Create the panel, passing the logged-in user's session data
            com.dentalclinic.admin.SystemLogPanel logsPanel = new com.dentalclinic.admin.SystemLogPanel(currentAdminId, isSuperAdmin);

            // 2. Use the existing showPanel helper to display it in the mainContent area
            showPanel(logsPanel);
        });

        
        JButton myAccountBtn = createSidebarButton("My Account Settings", 550);
        myAccountBtn.addActionListener(e -> {
            String roleStr = isSuperAdmin ? "Super Admin" : "Admin";
            showPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                currentAdminId, roleStr, currentAdminName, currentAdminUsername, currentAdminEmail
            ));
        });

        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(192, 57, 43)); // Red for Logout
        logoutBtn.addActionListener(e -> {
            new LoginPage();
            dispose();
        });

        sidebar.add(sysLogsBtn);
        sidebar.add(myAccountBtn);
        sidebar.add(logoutBtn);

        // --- MAIN CONTENT AREA ---
        mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(236, 240, 241));
        
        showPanel(dashboardStatsPanel);
        
        // Add both to the frame
        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);
        
        
        setVisible(true);
    }

    private void showPanel(JPanel panel) {
        mainContent.removeAll();
        mainContent.add(panel, BorderLayout.CENTER);
        mainContent.revalidate();
        mainContent.repaint();
    }

    private void toggleMenu() {
        isOpen = !isOpen;
        subMenuPanel.setVisible(isOpen);
        int shift = isOpen ? 85 : 0;
        accessControlBtn.setText(isOpen ? "Access Control  ⌃" : "Access Control  ⌄");
        sysLogsBtn.setLocation(sysLogsBtn.getX(), LOGS_Y + shift);
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
        return btn;
    }

    private void openMyProfile() {
        // Logic to open EditUserDialog with currentAdminId details
        JOptionPane.showMessageDialog(this, "Fetching your profile...");
    }
}