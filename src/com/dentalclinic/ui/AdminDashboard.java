package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.admin.AdminDashboardPanel;
import com.dentalclinic.admin.ClinicSettingsPanel;
import com.dentalclinic.admin.ManageUsersPanel;
import com.dentalclinic.util.UserSession;

public class AdminDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainContent; 
    private JButton accessControlBtn, sysLogsBtn, auditBtn, logoutBtn;
    private JPanel subMenuPanel;
    private boolean isOpen = false;
    private JButton clinicConfigBtn;
    
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String currentAdminName;  
    private String currentAdminEmail; 
    private String currentAdminUsername;
    private ManageUsersPanel manageUsersPanel;
    private AdminDashboardPanel dashboardStatsPanel;
    
    private final int CONFIG_Y = 200; 
    private final int ACCESS_Y = 250; 
    private final int LOGS_Y = 305;   
    private final int LOGOUT_Y = 600;

    public AdminDashboard(int loggedUserId, boolean isSuper, String fullName, String email, String username) {
        this.currentAdminId = loggedUserId;
        this.isSuperAdmin = isSuper;
        this.currentAdminName = fullName; 
        this.currentAdminEmail = email; 
        this.currentAdminUsername = username;
        
        this.manageUsersPanel = new ManageUsersPanel(this.currentAdminId, this.isSuperAdmin);
        this.dashboardStatsPanel = new AdminDashboardPanel();

        setTitle("Dental Clinic - Administrator Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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

        // --- SIDEBAR BUTTONS WITH PERMISSION CHECKS ---
        
        // 1. Dashboard Stats
        if (UserSession.hasPermission("VIEW_DASHBOARD")) {
            JButton myDashBtn = createSidebarButton("My Dashboard", 100);
            myDashBtn.addActionListener(e -> {
                showPanel(dashboardStatsPanel);
                dashboardStatsPanel.refreshStats(); 
            });
            sidebar.add(myDashBtn);
        }
        
        // 2. Audit Trails
        if (UserSession.hasPermission("VIEW_AUDIT_LOGS")) {
            auditBtn = createSidebarButton("Audit Trails", 150);
            auditBtn.addActionListener(e -> showPanel(new com.dentalclinic.admin.AuditTrailsPanel(currentAdminId, isSuperAdmin)));
            sidebar.add(auditBtn);
        }
        
        // 3. Clinic Configuration
        if (UserSession.hasPermission("MANAGE_CLINIC_SETTINGS")) {
            clinicConfigBtn = createSidebarButton("Clinic Configuration", CONFIG_Y);
            clinicConfigBtn.addActionListener(e -> showPanel(new ClinicSettingsPanel(currentAdminId, isSuperAdmin)));
            sidebar.add(clinicConfigBtn);
        }

        // 4. Access Control Section
        if (UserSession.hasPermission("MANAGE_USERS") || UserSession.hasPermission("MANAGE_ROLES")) {
            accessControlBtn = createSidebarButton("Access Control  ⌄", ACCESS_Y);
            accessControlBtn.addActionListener(e -> toggleMenu());
            sidebar.add(accessControlBtn);

            subMenuPanel = new JPanel(null);
            subMenuPanel.setBackground(new Color(34, 49, 63)); 
            subMenuPanel.setBounds(20, 295, 210, 80); 
            subMenuPanel.setVisible(false);

        if (UserSession.hasPermission("MANAGE_USERS")) {
                        JButton manageUsersBtn = createSubButton("Manage Users", 5);
                        manageUsersBtn.addActionListener(e -> {
                            showPanel(manageUsersPanel);
                            manageUsersPanel.refreshTable();
                        });
                        subMenuPanel.add(manageUsersBtn);
                    }
            
            if (UserSession.hasPermission("MANAGE_ROLES")) {
                JButton rolesBtn = createSubButton("Roles & Permissions", 40);
                rolesBtn.addActionListener(e -> showPanel(new com.dentalclinic.admin.ManageRolesPanel()));
                subMenuPanel.add(rolesBtn);
            }
            sidebar.add(subMenuPanel);
        }

        // 5. System Logs
        if (UserSession.hasPermission("VIEW_SYSTEM_LOGS")) {
            sysLogsBtn = createSidebarButton("System Logs", LOGS_Y);
            sysLogsBtn.addActionListener(e -> showPanel(new com.dentalclinic.admin.SystemLogPanel(currentAdminId, isSuperAdmin)));
            sidebar.add(sysLogsBtn);
        }

        // 6. Account Settings (Always Visible)
        JButton myAccountBtn = createSidebarButton("My Account Settings", 550);
        myAccountBtn.addActionListener(e -> {
            String roleStr = isSuperAdmin ? "Super Admin" : "Admin";
            showPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                currentAdminId, roleStr, currentAdminName, currentAdminUsername, currentAdminEmail
            ));
        });
        sidebar.add(myAccountBtn);

        // 7. Logout (Always Visible)
        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(192, 57, 43));
        logoutBtn.addActionListener(e -> {
            new LoginPage();
            dispose();
        });
        sidebar.add(logoutBtn);

        // --- MAIN CONTENT AREA ---
        mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(236, 240, 241));
        
        showPanel(dashboardStatsPanel);
        
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
        if (subMenuPanel == null) return;

        isOpen = !isOpen;
        subMenuPanel.setVisible(isOpen);
        int shift = isOpen ? 85 : 0;
        accessControlBtn.setText(isOpen ? "Access Control  ⌃" : "Access Control  ⌄");
        
        if (sysLogsBtn != null) {
            sysLogsBtn.setLocation(sysLogsBtn.getX(), LOGS_Y + shift);
        }
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