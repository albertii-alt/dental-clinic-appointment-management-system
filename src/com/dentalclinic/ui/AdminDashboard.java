package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.admin.AdminDashboardPanel;
import com.dentalclinic.admin.ClinicSettingsPanel;
import com.dentalclinic.admin.ManageUsersPanel;
import com.dental.clinic.ui.components.LogoutDialog;
import com.dentalclinic.admin.AuditTrailsPanel;
import com.dentalclinic.admin.ManageRolesPanel;
import com.dentalclinic.admin.SystemLogPanel;
import com.dentalclinic.admin.ReportsPanel;
import com.dentalclinic.ui.components.Sidebar;
import com.dentalclinic.ui.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboard extends JFrame {
    
    private JPanel mainContent;
    private Sidebar sidebar;
    private Timer sessionCheckTimer;
    
    private int currentAdminId;
    private boolean isSuperAdmin;
    private String currentAdminName;
    private String currentAdminEmail;
    private String currentAdminUsername;
    
    // Panels
    private ManageUsersPanel manageUsersPanel;
    private AdminDashboardPanel dashboardStatsPanel;
    private ClinicSettingsPanel clinicSettingsPanel;
    private ManageRolesPanel manageRolesPanel;
    private AuditTrailsPanel auditTrailsPanel;
    private SystemLogPanel systemLogPanel;
    private ReportsPanel reportsPanel;
    
    // Sidebar components
    private SidebarButton myDashBtn, auditBtn, clinicConfigBtn, accessControlBtn, sysLogsBtn, reportsBtn;
    private JPanel subMenuPanel;
    private SidebarButton manageUsersBtn, rolesBtn;
    private List<JComponent> componentsToShift = new ArrayList<>();
    private boolean isSubMenuOpen = false;
    private int shiftAmount = 85;

    public AdminDashboard(int loggedUserId, boolean isSuper, String fullName, String email, String username) {
        
        if (!UserSession.isSessionValid()) {
            new LoginPage();
            dispose();
            return;
        }
        
        this.currentAdminId = loggedUserId;
        this.isSuperAdmin = isSuper;
        this.currentAdminName = fullName;
        this.currentAdminEmail = email;
        this.currentAdminUsername = username;
        
        this.dashboardStatsPanel = new AdminDashboardPanel();
        
        setTitle("Dental Clinic - Administrator Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // Create sidebar
        sidebar = new Sidebar();
        sidebar.addLogo("Admin Panel", () -> {
            showPanel(dashboardStatsPanel);
            sidebar.clearActiveButton();
            dashboardStatsPanel.refreshStats();
            UserSession.updateActivity();
        });
        
        // ==========================================================
        // TOP SECTION (Never shifts)
        // ==========================================================
        
        // 1. My Dashboard
        if (UserSession.hasPermission("VIEW_DASHBOARD")) {
            myDashBtn = sidebar.addButton("My Dashboard", () -> {
                showPanel(dashboardStatsPanel);
                dashboardStatsPanel.refreshStats();
                UserSession.updateActivity();
            });
        }
        
        // 2. Audit Trails
        if (UserSession.hasPermission("VIEW_AUDIT_LOGS")) {
            auditBtn = sidebar.addButtonAt("Audit Trails", 150, () -> {
                if (auditTrailsPanel == null) {
                    auditTrailsPanel = new AuditTrailsPanel(currentAdminId, isSuperAdmin);
                }
                showPanel(auditTrailsPanel);
                UserSession.updateActivity();
            });
        }
        
        // 3. Clinic Configuration
        if (UserSession.hasPermission("MANAGE_CLINIC_SETTINGS")) {
            clinicConfigBtn = sidebar.addButtonAt("Clinic Configuration", 200, () -> {
                if (clinicSettingsPanel == null) {
                    clinicSettingsPanel = new ClinicSettingsPanel(currentAdminId, isSuperAdmin);
                }
                showPanel(clinicSettingsPanel);
                UserSession.updateActivity();
            });
        }
        
        // ==========================================================
        // ACCESS CONTROL SECTION (With Submenu)
        // ==========================================================
        
        if (UserSession.hasPermission("MANAGE_USERS") || UserSession.hasPermission("MANAGE_ROLES")) {
            accessControlBtn = new SidebarButton("Access Control  ⌄");
            accessControlBtn.setBounds(20, 250, 210, 40);
            accessControlBtn.addActionListener(e -> toggleSubMenu());
            sidebar.add(accessControlBtn);
            
            subMenuPanel = sidebar.createSubMenu(295, 80);
            
            if (UserSession.hasPermission("MANAGE_USERS")) {
                manageUsersBtn = sidebar.addSubButton(subMenuPanel, "Manage Users", 5, () -> {
                    if (manageUsersPanel == null) {
                        manageUsersPanel = new ManageUsersPanel(currentAdminId, isSuperAdmin);
                    }
                    showPanel(manageUsersPanel);
                    manageUsersPanel.refreshTable();
                    UserSession.updateActivity();
                });
            }
            
            if (UserSession.hasPermission("MANAGE_ROLES")) {
                rolesBtn = sidebar.addSubButton(subMenuPanel, "Roles & Permissions", 40, () -> {
                    if (manageRolesPanel == null) {
                        manageRolesPanel = new ManageRolesPanel(currentAdminId, isSuper);
                    }
                    showPanel(manageRolesPanel);
                    UserSession.updateActivity();
                });
            }
        }
        
        // ==========================================================
        // MIDDLE SECTION (Shifts when submenu opens)
        // ==========================================================
        
        // 4. System Logs (This will shift)
        if (UserSession.hasPermission("VIEW_SYSTEM_LOGS")) {
            sysLogsBtn = sidebar.addButtonAt("System Logs", 305, () -> {
                if (systemLogPanel == null) {
                    systemLogPanel = new SystemLogPanel(currentAdminId, isSuperAdmin);
                }
                showPanel(systemLogPanel);
                UserSession.updateActivity();
            });
        }
        
        // 5. Reports (NEW - This will also shift with System Logs)
        if (UserSession.hasPermission("VIEW_SYSTEM_LOGS")) { // Use appropriate permission
            reportsBtn = sidebar.addButtonAt("Reports", 355, () -> {
                if (reportsPanel == null) {
                    reportsPanel = new ReportsPanel(currentAdminId, isSuperAdmin);
                }
                showPanel(reportsPanel);
                UserSession.updateActivity();
            });
        }
        
        // Register components that shift when submenu opens
        if (sysLogsBtn != null) {
            componentsToShift.add(sysLogsBtn);
        }
        if (reportsBtn != null) {
            componentsToShift.add(reportsBtn);
        }
        
        // ==========================================================
        // BOTTOM SECTION (NEVER shifts - grouped with Logout)
        // ==========================================================
        
        // 6. My Account Settings (Moved to bottom section - DOES NOT SHIFT)
        SidebarButton myAccountBtn = new SidebarButton("My Account Settings");
        myAccountBtn.setBounds(20, 550, 210, 40);
        myAccountBtn.addActionListener(e -> {
            String roleStr = isSuperAdmin ? "Super Admin" : "Admin";
            showPanel(new com.dentalclinic.admin.AccountSettingsPanel(
                currentAdminId, roleStr, currentAdminName, currentAdminUsername, currentAdminEmail
            ));
            sidebar.setActiveButton(myAccountBtn);
            UserSession.updateActivity();
        });
        sidebar.add(myAccountBtn);
        
        // 7. Logout (Bottom - NEVER shifts)
        sidebar.addSpecialButton("Logout", 600, new Color(192, 57, 43), () -> {
            boolean confirm = LogoutDialog.show(this);
            if (confirm) {
                if (sessionCheckTimer != null) sessionCheckTimer.stop();
                UserSession.logout();
                new LoginPage();
                dispose();
            }
        });
        
        // Main content area
        mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(236, 240, 241));
        mainContent.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });
        
        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);
        
        showPanel(dashboardStatsPanel);
        startSessionMonitor();
        
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
    
    private void showPanel(JPanel newPanel) {
        mainContent.removeAll();
        mainContent.add(newPanel, BorderLayout.CENTER);
        mainContent.revalidate();
        mainContent.repaint();
        UserSession.updateActivity();
    }
    
    private void toggleSubMenu() {
        isSubMenuOpen = !isSubMenuOpen;
        subMenuPanel.setVisible(isSubMenuOpen);
        accessControlBtn.setText(isSubMenuOpen ? "Access Control  ⌃" : "Access Control  ⌄");
        
        int shift = isSubMenuOpen ? shiftAmount : -shiftAmount;
        
        // Only shift the components in the middle section
        for (JComponent comp : componentsToShift) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        sidebar.repaint();
        UserSession.updateActivity();
    }
}