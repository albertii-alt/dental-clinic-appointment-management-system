package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;

public class PatientDashboard extends JFrame {

    private JPanel sidebar;
    private JButton logoutBtn;
    private JButton bookBtn, todayBtn, requestBtn, historyBtn, cancelledBtn, notificationBtn, profileBtn;
    private JPanel contentArea;
    private int pID;
    private String pfName, pmName, plName, pdob,pAge, pAddress, pContact, pUsername;
    private int notificationCount = 0; // Class variable

    public PatientDashboard(int pID, String fName, String mName, String lName, String dob, String age, String addr, String phone, String user) {
        this.pID = pID; // Save the ID here!
        this.pfName = fName;
        this.pmName = mName;
        this.plName = lName;
        this.pdob = dob;
        this.pAge = age;
        this.pAddress = addr;
        this.pContact = phone;
        this.pUsername = user;

    // ... rest of your existing setup code (setTitle, setSize, etc.)

    // ... rest of your existing setup code (setTitle, setSize, etc.)        setTitle("Dental Clinic - Patient Portal");
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

        // Header matching your Sample UI
        JLabel logoLabel = new JLabel("Patient Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);
        
        // --- SIDEBAR BUTTONS ---
        bookBtn = createSidebarButton("Book Appointment", 100);
        todayBtn = createSidebarButton("Today's Schedule", 150); // Renamed for clarity
        requestBtn = createSidebarButton("My Appointment Requests", 200);
        historyBtn = createSidebarButton("My Appointment History", 250);
        cancelledBtn = createSidebarButton("Cancelled List", 300);
        // NEW BUTTON HERE
        notificationBtn = createSidebarButton("Notifications", 350); 
        notificationBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientNotificationPanel(pID, this));
        });
        profileBtn = createSidebarButton("Profile", 400);

        sidebar.add(bookBtn);
        sidebar.add(todayBtn);
        sidebar.add(requestBtn);
        sidebar.add(historyBtn);
        sidebar.add(cancelledBtn);
        sidebar.add(notificationBtn);
        sidebar.add(profileBtn);

        // --- LOGOUT ---
        logoutBtn = createSidebarButton("Logout", 600);
        logoutBtn.setBackground(new Color(192, 57, 43)); // Red for Logout
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- CONTENT AREA (Update this part) ---
        contentArea = new JPanel(new GridBagLayout()); // Use the class variable here
        contentArea.setBackground(new Color(236, 240, 241));
        
        renderDashboardHome(); 

        mainPanel.add(contentArea, BorderLayout.CENTER);

        // Logout Action
        logoutBtn.addActionListener(e -> { 
            new LoginPage(); 
            dispose(); 
        });
            // --- ACTION LISTENERS ---
        bookBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.BookAppointmentPanel(
                pID, pfName, pmName, plName, pdob, pAge, pAddress, pContact
            ));
        });

        todayBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientTodayPanel(pID));
        });

        requestBtn.addActionListener(e -> {
            // This replaces your old 'View Appointments' logic
            showPanel(new com.dentalclinic.patient.ViewAppointmentsPanel(pID));
        });

        historyBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientHistoryPanel(pID));
        });

        cancelledBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientCancelledPanel(pID));
        });

        profileBtn.addActionListener(e -> {
            showPanel(new com.dentalclinic.patient.PatientProfilePanel(pID));
        });
        
        refreshNotificationBadge();
        setVisible(true);
    }
    private void showPanel(JPanel panel) {
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(panel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
    }

    private JButton createSidebarButton(String text, int yPos) {
        // We override paintComponent specifically for the Notifications button
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Only draw if this is the notification button and count > 0
                if (getText().equals("Notifications") && notificationCount > 0) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Draw Red Circle
                    g2.setColor(Color.RED);
                    g2.fillOval(getWidth() - 30, 10, 20, 20);

                    // Draw White Number
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
        return button;
    }
    
    private void renderDashboardHome() {
    contentArea.removeAll();
    contentArea.setLayout(new BorderLayout(20, 20));
    contentArea.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

    // Top Welcome Header
    JLabel welcomeMsg = new JLabel("Welcome back, " + pfName + "!");
    welcomeMsg.setFont(new Font("SansSerif", Font.BOLD, 26));
    contentArea.add(welcomeMsg, BorderLayout.NORTH);

    // Main Dashboard Container (Grid for cards)
    JPanel cardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
    cardsPanel.setOpaque(false);

    // 1. Notifications Section
    cardsPanel.add(createDashboardCard("Recent Updates & Alerts", getNotificationItems()));

    // 2. Upcoming Appointments Section
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
                    item.setBackground(new Color(255, 243, 205)); // Light warning yellow
                    item.addActionListener(e -> {
                        try { 
                            dao.markAsRead(app.getAppointmentId()); 
                            renderDashboardHome(); // Refresh
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
            notificationBtn.repaint(); // Redraw the button with the new number
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}