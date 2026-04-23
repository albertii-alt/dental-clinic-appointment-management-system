package com.dentalclinic.view;

import javax.swing.*;
import java.awt.*;
import com.dentalclinic.view.admin.AdminDashboardPanel;
import com.dentalclinic.view.admin.ClinicSettingsPanel;
import com.dentalclinic.view.admin.ManageUsersPanel;
import com.dentalclinic.view.components.LogoutDialog;
import com.dentalclinic.view.admin.AuditTrailsPanel;
import com.dentalclinic.view.admin.ManageRolesPanel;
import com.dentalclinic.view.admin.SystemLogPanel;
import com.dentalclinic.view.admin.ReportsPanel;
import com.dentalclinic.view.components.Sidebar;
import com.dentalclinic.view.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
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
        
        // Create sidebar
        sidebar = new Sidebar();
        sidebar.addLogo("Admin Panel", currentAdminName, isSuperAdmin ? "Super Admin" : "Admin", () -> {
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
            myDashBtn = sidebar.addButton("My Dashboard", FontAwesomeSolid.TACHOMETER_ALT, () -> {
                showPanel(dashboardStatsPanel);
                dashboardStatsPanel.refreshStats();
                UserSession.updateActivity();
            });
        }
        
        // 2. Audit Trails
        if (UserSession.hasPermission("VIEW_AUDIT_LOGS")) {
            auditBtn = sidebar.addButtonAt("Audit Trails", FontAwesomeSolid.HISTORY, 150, () -> {
                if (auditTrailsPanel == null) {
                    auditTrailsPanel = new AuditTrailsPanel(currentAdminId, isSuperAdmin);
                }
                showPanel(auditTrailsPanel);
                UserSession.updateActivity();
            });
        }
        
        // 3. Clinic Configuration
        if (UserSession.hasPermission("MANAGE_CLINIC_SETTINGS")) {
            clinicConfigBtn = sidebar.addButtonAt("Clinic Configuration", FontAwesomeSolid.COGS, 200, () -> {
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
            accessControlBtn = new SidebarButton("Access Control");
            accessControlBtn.setIcon(FontAwesomeSolid.LOCK);
            accessControlBtn.setTrailingIcon(FontAwesomeSolid.CHEVRON_DOWN);
            accessControlBtn.setBounds(20, 250, 210, 40);
            accessControlBtn.addActionListener(e -> toggleSubMenu());
            sidebar.add(accessControlBtn);
            
            subMenuPanel = sidebar.createSubMenu(295, 80);
            
            if (UserSession.hasPermission("MANAGE_USERS")) {
                manageUsersBtn = sidebar.addSubButton(subMenuPanel, "Manage Users", FontAwesomeSolid.USERS, 5, () -> {
                    if (manageUsersPanel == null) {
                        manageUsersPanel = new ManageUsersPanel(currentAdminId, isSuperAdmin);
                    }
                    showPanel(manageUsersPanel);
                    manageUsersPanel.refreshTable();
                    UserSession.updateActivity();
                });
            }
            
            if (UserSession.hasPermission("MANAGE_ROLES")) {
                rolesBtn = sidebar.addSubButton(subMenuPanel, "Roles & Permissions", FontAwesomeSolid.USER_TAG, 40, () -> {
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
            sysLogsBtn = sidebar.addButtonAt("System Logs", FontAwesomeSolid.FILE_ALT, 305, () -> {
                if (systemLogPanel == null) {
                    systemLogPanel = new SystemLogPanel(currentAdminId, isSuperAdmin);
                }
                showPanel(systemLogPanel);
                UserSession.updateActivity();
            });
        }
        
        // 5. Reports
        if (UserSession.hasPermission("VIEW_SYSTEM_LOGS")) {
            reportsBtn = sidebar.addButtonAt("Reports", FontAwesomeSolid.CHART_LINE, 355, () -> {
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
        
        // 6. My Account Settings - bottom pinned
        final SidebarButton[] myAccountBtnRef = new SidebarButton[1];
        myAccountBtnRef[0] = sidebar.addBottomButton("My Account Settings", FontAwesomeSolid.USER_COG, () -> {
            String roleStr = isSuperAdmin ? "Super Admin" : "Admin";
            showPanel(new com.dentalclinic.view.admin.AccountSettingsPanel(
                currentAdminId, roleStr, currentAdminName, currentAdminUsername, currentAdminEmail
            ));
            sidebar.setActiveButton(myAccountBtnRef[0]);
            UserSession.updateActivity();
        });
        SidebarButton myAccountBtn = myAccountBtnRef[0];
        
        // 7. Logout - bottom pinned
        sidebar.addSpecialButton("Logout", 0, new Color(192, 57, 43), () -> {
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
        accessControlBtn.setTrailingIcon(isSubMenuOpen ? FontAwesomeSolid.CHEVRON_UP : FontAwesomeSolid.CHEVRON_DOWN);
        
        int shift = isSubMenuOpen ? shiftAmount : -shiftAmount;
        
        for (JComponent comp : componentsToShift) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        sidebar.repaint();
        UserSession.updateActivity();
    }
}
