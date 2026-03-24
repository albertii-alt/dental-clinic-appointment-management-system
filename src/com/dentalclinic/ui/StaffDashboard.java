package com.dentalclinic.ui;

import com.dentalclinic.staff.CancelledAppointmentsPanel;
import javax.swing.*;
import java.awt.*;
import com.dentalclinic.staff.PendingRequestsPanel; // Import your panel
import com.dentalclinic.staff.TodaysAppointmentsPanel;
import com.dentalclinic.staff.UpcomingAppointmentsPanel;

public class StaffDashboard extends JFrame {

    private JPanel sidebar;
    private JPanel mainPanel; // Moved to class level
    private JPanel currentContent; // To track what's currently in the center
    private JButton logoutBtn;
    private int staffId;
    private String staffName;
    private String role = "Staff";

    public StaffDashboard(int staffId, String staffName) {
        this.staffId = staffId;
        this.staffName = staffName;
        setTitle("Dental Clinic - Staff Dashboard");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());
        add(mainPanel);

        // --- SIDEBAR PANEL ---
        sidebar = new JPanel();
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(250, 700));
        sidebar.setLayout(null);

        JLabel logoLabel = new JLabel("Staff Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- CREATE BUTTONS AND ADD ACTIONS ---
        
        // 1. Pending Appointments Button
        JButton pendingBtn = createSidebarButton("Pending Appointments", 150);
        pendingBtn.addActionListener(e -> switchPanel(new PendingRequestsPanel()));
        sidebar.add(pendingBtn);

        // Add placeholders for other buttons (you can add panels for these later)
        JButton todayBtn = createSidebarButton("Today's Appointments", 100);
        todayBtn.addActionListener(e -> switchPanel(new TodaysAppointmentsPanel()));
        sidebar.add(todayBtn);
        
        JButton btnCancelled = createSidebarButton("Cancelled Appointments", 200);
        btnCancelled.addActionListener(e -> switchPanel(new CancelledAppointmentsPanel())); 
        sidebar.add(btnCancelled);

        JButton upcomingBtn = createSidebarButton("Upcoming Appointments", 250);
        upcomingBtn.addActionListener(e -> switchPanel(new UpcomingAppointmentsPanel()));
        sidebar.add(upcomingBtn);
        
        
        JLabel patientLabel = new JLabel("Management");
        patientLabel.setForeground(new Color(171, 183, 183));
        patientLabel.setBounds(25, 305, 150, 20);
        sidebar.add(patientLabel);

        JButton regBtn = createSidebarButton("Register Patient", 330);
        regBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.RegisterPatientPanel()));
        sidebar.add(regBtn);
        
        JButton createBtn = createSidebarButton("Create Appointment", 380);
        createBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.StaffBookAppointmentPanel()));
        sidebar.add(createBtn);
        
        JButton historyBtn = createSidebarButton("View Patient History", 430);
        // False because Staff cannot edit clinical records
        historyBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.PatientHistoryPanel(false)));
        sidebar.add(historyBtn);
        
        JButton manageSchedBtn = createSidebarButton("Manage Schedule", 480);
        manageSchedBtn.addActionListener(e -> switchPanel(new com.dentalclinic.staff.StaffManageSchedulePanel(staffId, staffName, role)));
        sidebar.add(manageSchedBtn);

        logoutBtn = createSidebarButton("Logout", 600);
        logoutBtn.setBackground(new Color(192, 57, 43));
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- INITIAL WELCOME CONTENT ---
        showWelcomeScreen();

        // --- LOGOUT ACTION ---
        logoutBtn.addActionListener(e -> {
             new LoginPage(); 
            dispose();
        });

        setVisible(true);
    }

    // HELPER METHOD TO SWITCH PANELS
    private void switchPanel(JPanel newPanel) {
        if (currentContent != null) {
            mainPanel.remove(currentContent);
        }
        currentContent = newPanel;
        mainPanel.add(currentContent, BorderLayout.CENTER);
        mainPanel.revalidate(); // Refresh layout
        mainPanel.repaint();    // Redraw screen
    }

    private void showWelcomeScreen() {
        JPanel welcomePanel = new JPanel(new GridBagLayout());
        welcomePanel.setBackground(new Color(236, 240, 241));
        
        JLabel welcomeMsg = new JLabel("Welcome, " + staffName);
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));
        
        JLabel subMsg = new JLabel("Select an appointment category to manage the clinic flow");
        subMsg.setFont(new Font("Arial", Font.PLAIN, 18));
        subMsg.setForeground(Color.GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        welcomePanel.add(welcomeMsg, gbc);
        gbc.gridy = 1;
        welcomePanel.add(subMsg, gbc);

        switchPanel(welcomePanel);
    }

    private JButton createSidebarButton(String text, int yPos) {
        JButton button = new JButton(text);
        button.setBounds(20, yPos, 210, 40);
        button.setFocusPainted(false);
        button.setBackground(new Color(52, 152, 219));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
    
}