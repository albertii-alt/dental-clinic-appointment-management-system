package com.dentalclinic.ui;

import com.dentalclinic.ui.components.LogoutDialog;
import com.dentalclinic.ui.components.Sidebar;
import com.dentalclinic.ui.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javax.swing.*;
import java.awt.*;

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
            renderDashboardHome();
            sidebar.clearActiveButton();
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Book Appointment", FontAwesomeSolid.CALENDAR_PLUS, () -> {
            showPanel(new com.dentalclinic.patient.BookAppointmentPanel(
                pID, pfName, pmName, plName, pdob, pAge, pAddress, pContact
            ));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Today's Schedule", FontAwesomeSolid.CALENDAR_DAY, () -> {
            showPanel(new com.dentalclinic.patient.PatientTodayPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Upcoming Visits", FontAwesomeSolid.CALENDAR_WEEK, () -> {
            showPanel(new com.dentalclinic.patient.PatientUpcomingPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("My Appointment Requests", FontAwesomeSolid.CLOCK, () -> {
            showPanel(new com.dentalclinic.patient.ViewAppointmentsPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Medical History", FontAwesomeSolid.NOTES_MEDICAL, () -> {
            showPanel(new com.dentalclinic.patient.PatientHistoryPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Cancelled List", FontAwesomeSolid.BAN, () -> {
            showPanel(new com.dentalclinic.patient.PatientCancelledPanel(pID));
            UserSession.updateActivity();
        });
        
        notificationBtn = sidebar.addButton("Notifications", FontAwesomeSolid.BELL, () -> {
            showPanel(new com.dentalclinic.patient.PatientNotificationPanel(pID, this));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Profile", FontAwesomeSolid.USER, () -> {
            showPanel(new com.dentalclinic.patient.PatientProfilePanel(pID));
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
        renderDashboardHome();
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
    
    private void showPanel(JPanel panel) {
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }
    
    private void renderDashboardHome() {
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout(20, 20));
        contentArea.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel welcomeMsg = new JLabel("Welcome back, " + pfName + "!");
        welcomeMsg.setFont(new Font("SansSerif", Font.BOLD, 26));
        contentArea.add(welcomeMsg, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.add(createDashboardCard("Recent Updates & Alerts", getNotificationItems()));
        cardsPanel.add(createDashboardCard("Upcoming Visits", getUpcomingItems()));

        contentArea.add(cardsPanel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JPanel createDashboardCard(String title, JPanel listContent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(new JScrollPane(listContent), BorderLayout.CENTER);
        return card;
    }

    private JPanel getNotificationItems() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        try {
            com.dentalclinic.dao.AppointmentDAO dao = new com.dentalclinic.dao.AppointmentDAO();
            java.util.List<com.dentalclinic.model.Appointment> unread = dao.getUnreadNotifications(pID);

            if (unread.isEmpty()) {
                panel.add(new JLabel("No new alerts."));
            } else {
                for (com.dentalclinic.model.Appointment app : unread) {
                    String msg = "<html><b>" + app.getStatus() + ":</b> Your " + app.getServiceType() + 
                                 " on " + app.getAppointmentDate() + " was updated.<br>" +
                                 "<i>Note: " + (app.getClinicalNotes() != null ? app.getClinicalNotes() : "No reason provided") + "</i></html>";

                    JButton item = new JButton(msg);
                    item.setHorizontalAlignment(SwingConstants.LEFT);
                    item.setBackground(new Color(255, 243, 205));
                    item.addActionListener(e -> {
                        try { 
                            dao.markAsRead(app.getAppointmentId()); 
                            renderDashboardHome();
                            refreshNotificationBadge();
                            UserSession.updateActivity();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                    panel.add(item);
                    panel.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return panel;
    }

    private JPanel getUpcomingItems() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);

        try {
            com.dentalclinic.dao.AppointmentDAO dao = new com.dentalclinic.dao.AppointmentDAO();
            java.util.List<com.dentalclinic.model.Appointment> upcoming = dao.getFutureUpcoming(pID);

            if (upcoming.isEmpty()) {
                panel.add(new JLabel("No upcoming appointments."));
            } else {
                for (com.dentalclinic.model.Appointment app : upcoming) {
                    JLabel lbl = new JLabel("<html>📅 " + app.getAppointmentDate() + " at " + app.getAppointmentTime() + 
                                           "<br><b>" + app.getServiceType() + "</b></html>");
                    lbl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    panel.add(lbl);
                    panel.add(new JSeparator());
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return panel;
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