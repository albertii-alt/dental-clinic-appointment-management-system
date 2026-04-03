package com.dentalclinic.ui;

import com.dental.clinic.ui.components.LogoutDialog;
import com.dentalclinic.util.UserSession;
import javax.swing.*;
import java.awt.*;

public class PatientDashboard extends JFrame {

    private JPanel sidebar;
    private JButton logoutBtn;
    private JButton bookBtn, todayBtn, requestBtn, historyBtn, cancelledBtn, notificationBtn, profileBtn;
    private JPanel contentArea;
    private int pID;
    private String pfName, pmName, plName, pdob, pAge, pAddress, pContact, pUsername;
    private int notificationCount = 0;
    private Timer sessionCheckTimer; // Store timer reference to stop on logout
    private final int LOGOUT_Y = 600;

    public PatientDashboard(int pID, String fName, String mName, String lName, String dob, String age, String addr, String phone, String user) {
        
        // Check session validity before proceeding
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

        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // --- SIDEBAR PANEL ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80)); 
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Patient Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        logoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                renderDashboardHome();
                UserSession.updateActivity(); // Track activity
            }
        });
        sidebar.add(logoLabel);
        
        // --- SIDEBAR BUTTONS ---
        bookBtn = createSidebarButton("Book Appointment", 100);
        todayBtn = createSidebarButton("Today's Schedule", 150);
        JButton upcomingBtn = createSidebarButton("Upcoming Visits", 200);
        requestBtn = createSidebarButton("My Appointment Requests", 250);
        historyBtn = createSidebarButton("Medical History", 300);
        cancelledBtn = createSidebarButton("Cancelled List", 350);
        notificationBtn = createSidebarButton("Notifications", 400); 
        profileBtn = createSidebarButton("Profile", 450);

        sidebar.add(bookBtn);
        sidebar.add(todayBtn);
        sidebar.add(upcomingBtn);
        sidebar.add(requestBtn);
        sidebar.add(historyBtn);
        sidebar.add(cancelledBtn);
        sidebar.add(notificationBtn);
        sidebar.add(profileBtn);

        // --- LOGOUT BUTTON ---
        logoutBtn = createSidebarButton("Logout", LOGOUT_Y);
        logoutBtn.setBackground(new Color(192, 57, 43));
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- CONTENT AREA ---
        contentArea = new JPanel(new GridBagLayout());
        contentArea.setBackground(new Color(236, 240, 241));
        mainPanel.add(contentArea, BorderLayout.CENTER);

        // --- ACTION LISTENERS ---
        bookBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.BookAppointmentPanel(
                pID, pfName, pmName, plName, pdob, pAge, pAddress, pContact
            ));
            UserSession.updateActivity();
        });

        todayBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientTodayPanel(pID));
            UserSession.updateActivity();
        });

        requestBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.ViewAppointmentsPanel(pID));
            UserSession.updateActivity();
        });
        
        upcomingBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientUpcomingPanel(pID));
            UserSession.updateActivity();
        });

        historyBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientHistoryPanel(pID));
            UserSession.updateActivity();
        });

        cancelledBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientCancelledPanel(pID));
            UserSession.updateActivity();
        });

        notificationBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientNotificationPanel(pID, this));
            UserSession.updateActivity();
        });

        profileBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientProfilePanel(pID));
            UserSession.updateActivity();
        });

        // --- LOGOUT ACTION ---
        logoutBtn.addActionListener(e -> {
            boolean confirm = LogoutDialog.show(this);
            if (confirm) {
                // Stop the session timer
                if (sessionCheckTimer != null) {
                    sessionCheckTimer.stop();
                }
                // Clear session
                UserSession.logout();
                // Return to login
                new LoginPage();
                dispose();
            }
        });

        // Start session monitor (check every 10 seconds)
        startSessionMonitor();
        
        // Render dashboard
        renderDashboardHome();
        refreshNotificationBadge();
        
        // Auto-send reminders for tomorrow's appointments (runs in background)
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
                // Session expired
                sessionCheckTimer.stop();
                
                // Show message using your custom dialog
                com.dental.clinic.ui.components.ErrorDialog.show(this, 
                    "Session Expired", 
                    "Your session has expired due to inactivity.\nPlease login again.");
                
                // Clear session and redirect
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

    private JButton createSidebarButton(String text, int yPos) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().equals("Notifications") && notificationCount > 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.RED);
                    g2.fillOval(getWidth() - 30, 10, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Arial", Font.BOLD, 12));
                    String countStr = String.valueOf(notificationCount);
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - 30) + (20 - fm.stringWidth(countStr)) / 2;
                    int y = 10 + ((20 - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(countStr, x, y);
                }
            }
        };

        button.setBounds(20, yPos, 210, 40);
        button.setFocusPainted(false);
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Track activity on button hover
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                UserSession.updateActivity();
            }
        });
        
        return button;
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
            notificationBtn.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}