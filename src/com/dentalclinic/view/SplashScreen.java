package com.dentalclinic.view;

import com.dentalclinic.controller.AppointmentController;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class SplashScreen extends JWindow {

    private static final Color GRADIENT_START = new Color(20, 30, 48);
    private static final Color GRADIENT_END   = new Color(36, 59, 85);
    private static final Color ACCENT         = new Color(41, 128, 185);
    private static final Color TEXT_WHITE     = Color.WHITE;
    private static final Color TEXT_MUTED     = new Color(174, 214, 241);

    // Total splash duration in ms
    private static final int SPLASH_DURATION_MS = 2500;
    private static final int TIMER_INTERVAL_MS  = 20;

    private JProgressBar progressBar;
    private int currentProgress = 0;
    private Timer animationTimer;

    public SplashScreen() {
        setSize(480, 300);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, GRADIENT_START, getWidth(), getHeight(), GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(30, 40, 25, 40));

        // --- CENTER: logo + name ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel logoLabel = new JLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        try {
            InputStream is = getClass().getResourceAsStream("/com/dentalclinic/resources/VantageLogoInForms.png");
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {}
        centerPanel.add(logoLabel);
        centerPanel.add(Box.createVerticalStrut(14));

        JLabel clinicName = new JLabel("Vantage Dental");
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 26));
        clinicName.setForeground(TEXT_WHITE);
        clinicName.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(clinicName);
        centerPanel.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Appointment Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(subtitle);

        root.add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM: progress bar + version ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        progressBar.setPreferredSize(new Dimension(400, 6));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setBackground(new Color(255, 255, 255, 40));
        progressBar.setForeground(ACCENT);
        progressBar.setBorderPainted(false);
        progressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override protected Color getSelectionBackground() { return ACCENT; }
            @Override protected Color getSelectionForeground() { return ACCENT; }
        });
        bottomPanel.add(progressBar);
        bottomPanel.add(Box.createVerticalStrut(10));

        JLabel version = new JLabel("v1.0.0  |  © 2026 Vantage Dental Clinic");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        version.setForeground(new Color(127, 140, 141));
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(version);

        root.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    public void runStartupAndLaunch() {
        setVisible(true);

        // Run startup tasks silently in background
        new Thread(() -> {
            try {
                AppointmentController ac = new AppointmentController();
                ac.expirePastApprovedAppointments();
                ac.sendAllRemindersForTomorrow();
                ac.sendAllDayOfReminders();
            } catch (Exception e) {
                System.err.println("Startup task failed: " + e.getMessage());
            }
        }).start();

        // Smooth progress animation over SPLASH_DURATION_MS
        int totalSteps = SPLASH_DURATION_MS / TIMER_INTERVAL_MS;
        double increment = 100.0 / totalSteps;

        animationTimer = new Timer(TIMER_INTERVAL_MS, null);
        animationTimer.addActionListener(e -> {
            currentProgress += increment;
            if (currentProgress >= 100) {
                currentProgress = 100;
                progressBar.setValue(100);
                animationTimer.stop();
                // Small pause at 100% then launch
                Timer launchTimer = new Timer(200, ev -> {
                    dispose();
                    new LoginPage();
                });
                launchTimer.setRepeats(false);
                launchTimer.start();
            } else {
                progressBar.setValue(currentProgress);
            }
        });
        animationTimer.start();
    }
}
