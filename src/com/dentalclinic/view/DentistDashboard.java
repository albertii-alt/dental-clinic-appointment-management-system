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
import java.util.ArrayList;
import java.util.List;
import com.dentalclinic.view.components.WelcomePanel;

public class DentistDashboard extends JFrame {

    private JPanel mainPanel;
    private JPanel currentContent;
    private Sidebar sidebar;
    private Timer sessionCheckTimer;
    private JPanel appointmentsSubMenu;
    private SidebarButton viewAppBtn;
    private SidebarButton historyBtn;
    private SidebarButton blockBtn;
    private List<JComponent> componentsToShift = new ArrayList<>();
    private boolean isAppMenuOpen = false;
    private int shiftAmount = 85;
    
    private int staffId;
    private String staffName;
    private String username;
    private String email;
    private String role = "Dentist";

    public DentistDashboard(int staffId, String staffName, String user, String mail) {
        
        if (!UserSession.isSessionValid()) {
            new LoginPage();
            dispose();
            return;
        }
        
        this.staffId = staffId;
        this.staffName = staffName;
        this.username = user;
        this.email = mail;
        
        setTitle("Dental Clinic - Dentist Dashboard");
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
        sidebar.addLogo("Dentist Portal", () -> {
            switchPanel(new WelcomePanel(staffName, "See your Appointments for today, Doc!"));
            sidebar.clearActiveButton();
            UserSession.updateActivity();
        });
        // View Appointments Dropdown
        if (UserSession.hasPermission("MANAGE_APPOINTMENTS")) {
            viewAppBtn = new SidebarButton("View Appointments");
            viewAppBtn.setIcon(FontAwesomeSolid.CALENDAR_ALT);
            viewAppBtn.setTrailingIcon(FontAwesomeSolid.CHEVRON_DOWN);
            viewAppBtn.setBounds(20, 100, 210, 40);
            viewAppBtn.addActionListener(e -> toggleAppMenu());
            sidebar.add(viewAppBtn);
            
            appointmentsSubMenu = sidebar.createSubMenu(145, 80);
            
            sidebar.addSubButton(appointmentsSubMenu, "Today's Schedule", FontAwesomeSolid.CALENDAR_DAY, 5, () -> {
                switchPanel(new TodaysAppointmentsPanel());
                UserSession.updateActivity();
            });
            
            sidebar.addSubButton(appointmentsSubMenu, "Upcoming Treatments", FontAwesomeSolid.CALENDAR_WEEK, 40, () -> {
                switchPanel(new UpcomingAppointmentsPanel());
                UserSession.updateActivity();
            });
        }
        
        // View Patient History
        if (UserSession.hasPermission("VIEW_MEDICAL_HISTORY")) {
            historyBtn = sidebar.addButtonAt("View Patient History", FontAwesomeSolid.NOTES_MEDICAL, 150, () -> {
                switchPanel(new PatientHistoryPanel(true));
                UserSession.updateActivity();
            });
        }
        
        // Block Time Slots
        if (UserSession.hasPermission("MANAGE_SCHEDULE")) {
            blockBtn = sidebar.addButtonAt("Block Time Slots", FontAwesomeSolid.CLOCK, 200, () -> {
                switchPanel(new StaffManageSchedulePanel(staffId, staffName, role));
                UserSession.updateActivity();
            });
        }
        
        if (historyBtn != null) {
            componentsToShift.add(historyBtn);
        }
        if (blockBtn != null) {
            componentsToShift.add(blockBtn);
        }
        
        // My Account Settings
        sidebar.addButtonAt("My Account Settings", FontAwesomeSolid.USER_COG, 550, () -> {
            switchPanel(new com.dentalclinic.view.admin.AccountSettingsPanel(
                staffId, role, staffName, username, email
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
        switchPanel(new WelcomePanel(staffName, "See your Appointments for today, Doc!"));
        
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
    
    private void toggleAppMenu() {
        if (appointmentsSubMenu == null) return;
        
        isAppMenuOpen = !isAppMenuOpen;
        appointmentsSubMenu.setVisible(isAppMenuOpen);
        viewAppBtn.setTrailingIcon(isAppMenuOpen ? FontAwesomeSolid.CHEVRON_UP : FontAwesomeSolid.CHEVRON_DOWN);
        
        int shift = isAppMenuOpen ? shiftAmount : -shiftAmount;
        
        for (JComponent comp : componentsToShift) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        sidebar.repaint();
        UserSession.updateActivity();
    }
}
