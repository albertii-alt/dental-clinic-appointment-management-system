package com.dentalclinic.ui;

import javax.swing.*;
import java.awt.*;

public class PatientDashboard extends JFrame {

    private JPanel sidebar;
    private JButton logoutBtn;
    private JButton bookBtn, viewBtn, historyBtn, profileBtn;
    private JPanel contentArea; // Move this to a class variable

    public PatientDashboard(String patientName) {
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

        // Header matching your Sample UI
        JLabel logoLabel = new JLabel("Patient Portal");
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        sidebar.add(logoLabel);

        // --- BUTTONS BASED ON USE CASE ---
        bookBtn = createSidebarButton("Book Appointment", 100);
        viewBtn = createSidebarButton("View Appointments", 150);
        historyBtn = createSidebarButton("My Appointment History", 200);
        profileBtn = createSidebarButton("Profile", 250);

        sidebar.add(bookBtn);
        sidebar.add(viewBtn);
        sidebar.add(historyBtn);
        sidebar.add(profileBtn);

        // --- LOGOUT ---
        logoutBtn = createSidebarButton("Logout", 600);
        logoutBtn.setBackground(new Color(41, 128, 185));
        sidebar.add(logoutBtn);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // --- CONTENT AREA (Update this part) ---
        contentArea = new JPanel(new GridBagLayout()); // Use the class variable here
        contentArea.setBackground(new Color(236, 240, 241));
        
                // --- INITIAL WELCOME CONTENT ---
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BoxLayout(welcomePanel, BoxLayout.Y_AXIS));
        welcomePanel.setBackground(new Color(236, 240, 241));

        JLabel welcomeMsg = new JLabel("Welcome, " + patientName);
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));
        welcomeMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subMsg = new JLabel("Select an option from the sidebar to manage your dental care");
        subMsg.setFont(new Font("Arial", Font.PLAIN, 18));
        subMsg.setForeground(Color.GRAY);
        subMsg.setAlignmentX(Component.CENTER_ALIGNMENT);

        welcomePanel.add(welcomeMsg);
        welcomePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        welcomePanel.add(subMsg);

        // Add the welcome panel to the contentArea initially
        contentArea.add(welcomePanel);
mainPanel.add(contentArea, BorderLayout.CENTER);

        // Logout Action
        logoutBtn.addActionListener(e -> { 
            new LoginPage(); 
            dispose(); 
        });
                // Action for Book Appointment Button
        bookBtn.addActionListener(e -> {
            // 1. Clear the welcome message
            contentArea.removeAll(); 

            // 2. Change layout to BorderLayout so the form fills the space
            contentArea.setLayout(new BorderLayout()); 

            // 3. Add the BookAppointmentPanel (The one we made in the previous step)
            contentArea.add(new com.dentalclinic.patient.BookAppointmentPanel(), BorderLayout.CENTER);

            // 4. Refresh the UI to show changes
            contentArea.revalidate();
            contentArea.repaint();
        });
        setVisible(true);
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