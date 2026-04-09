package com.dentalclinic.ui;

import com.dentalclinic.ui.components.LogoutDialog;
import com.dentalclinic.ui.components.Sidebar;
import com.dentalclinic.ui.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javax.swing.*;
import java.awt.*;
import com.dentalclinic.ui.components.WelcomePanel;

public class PatientDashboard extends JFrame {

    private JPanel contentArea;
    private Sidebar sidebar;
    private Timer sessionCheckTimer;
    private SidebarButton notificationBtn;
    
    private int pID;
    private String pfName, pmName, plName, pdob, pAge, pAddress, pContact, pUsername;
    private int notificationCount = 0;

    public PatientDashboard(int pID, String fName, String mName, String lName, String dob, String age, String addr, String phone, String user) {
        
        if (!UserSession.isSessionValid()) {
            new LoginPage();
            dispose();
            return;
        }
        
        this.pID = pID;
        this.pfName = fName;
        this.pmName = mName;
        this.plName = lName;
        this.pdob = dob;
        this.pAge = age;
        this.pAddress = addr;
        this.pContact = phone;
        this.pUsername = user;
        
        setTitle("Dental Clinic - Patient Portal");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        sidebar = new Sidebar();
        sidebar.addLogo("Patient Portal", () -> {
            switchPanel(new WelcomePanel(pfName, "Select a category from the sidebar to manage your dental records"));
            sidebar.clearActiveButton();
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Book Appointment", FontAwesomeSolid.CALENDAR_PLUS, () -> {
            switchPanel(new com.dentalclinic.patient.BookAppointmentPanel(
                pID, pfName, pmName, plName, pdob, pAge, pAddress, pContact
            ));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Today's Schedule", FontAwesomeSolid.CALENDAR_DAY, () -> {
            switchPanel(new com.dentalclinic.patient.PatientTodayPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Upcoming Visits", FontAwesomeSolid.CALENDAR_WEEK, () -> {
            switchPanel(new com.dentalclinic.patient.PatientUpcomingPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("My Appointment Requests", FontAwesomeSolid.CLOCK, () -> {
            switchPanel(new com.dentalclinic.patient.ViewAppointmentsPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Medical History", FontAwesomeSolid.NOTES_MEDICAL, () -> {
            switchPanel(new com.dentalclinic.patient.PatientHistoryPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Cancelled List", FontAwesomeSolid.BAN, () -> {
            switchPanel(new com.dentalclinic.patient.PatientCancelledPanel(pID));
            UserSession.updateActivity();
        });
        
        notificationBtn = sidebar.addButton("Notifications", FontAwesomeSolid.BELL, () -> {
            switchPanel(new com.dentalclinic.patient.PatientNotificationPanel(pID, this));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Profile", FontAwesomeSolid.USER, () -> {
            switchPanel(new com.dentalclinic.patient.PatientProfilePanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addSpecialButton("Logout", 600, new Color(192, 57, 43), () -> {
            boolean confirm = LogoutDialog.show(this);
            if (confirm) {
                if (sessionCheckTimer != null) sessionCheckTimer.stop();
                UserSession.logout();
                new LoginPage();
                dispose();
            }
        });
        
        contentArea = new JPanel(new GridBagLayout());
        contentArea.setBackground(new Color(236, 240, 241));
        
        add(sidebar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);
        
        startSessionMonitor();
        switchPanel(new WelcomePanel(pfName, "Select a category from the sidebar to manage your dental records"));
        refreshNotificationBadge();
        
        new Thread(() -> {
            try {
                com.dentalclinic.service.AppointmentService appService = new com.dentalclinic.service.AppointmentService();
                int tomorrowSent = appService.sendAllRemindersForTomorrow();
                if (tomorrowSent > 0) {
                    System.out.println("Sent " + tomorrowSent + " appointment reminders for tomorrow");
                }
                int todaySent = appService.sendAllDayOfReminders();
                if (todaySent > 0) {
                    System.out.println("Sent " + todaySent + " day-of appointment reminders");
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
                com.dentalclinic.ui.components.ErrorDialog.show(this, 
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
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(newPanel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
        UserSession.updateActivity();
    }

    public void refreshNotificationBadge() {
        try {
            com.dentalclinic.dao.AppointmentDAO dao = new com.dentalclinic.dao.AppointmentDAO();
            this.notificationCount = dao.getUnreadNotificationCount(pID);
            notificationBtn.setNotificationCount(notificationCount);
            notificationBtn.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}