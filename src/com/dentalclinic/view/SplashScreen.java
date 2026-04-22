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

    private JProgressBar progressBar;
    private JLabel statusLabel;

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

        // Logo
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

        // Clinic name
        JLabel clinicName = new JLabel("Vantage Dental");
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 26));
        clinicName.setForeground(TEXT_WHITE);
        clinicName.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(clinicName);
        centerPanel.add(Box.createVerticalStrut(4));

        // Subtitle
        JLabel subtitle = new JLabel("Appointment Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(subtitle);

        root.add(centerPanel, BorderLayout.CENTER);

        // --- BOTTOM: progress bar + status + version ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        statusLabel = new JLabel("Starting up...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createVerticalStrut(5));

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
            @Override
            protected Color getSelectionBackground() { return ACCENT; }
            @Override
            protected Color getSelectionForeground() { return ACCENT; }
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

    private void setProgress(int value, String status) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            statusLabel.setText(status);
        });
    }

    public void runStartupAndLaunch() {
        setVisible(true);

        new Thread(() -> {
            try {
                setProgress(10, "Initializing...");
                Thread.sleep(300);

                AppointmentController ac = new AppointmentController();

                setProgress(30, "Checking appointments...");
                int expired = ac.expirePastApprovedAppointments();
                if (expired > 0) System.out.println("Auto-expired " + expired + " appointment(s)");

                setProgress(55, "Sending reminders...");
                int tomorrowSent = ac.sendAllRemindersForTomorrow();
                if (tomorrowSent > 0) System.out.println("Sent " + tomorrowSent + " reminder(s) for tomorrow");

                setProgress(75, "Sending day-of reminders...");
                int todaySent = ac.sendAllDayOfReminders();
                if (todaySent > 0) System.out.println("Sent " + todaySent + " day-of reminder(s)");

                setProgress(90, "Loading login screen...");
                Thread.sleep(400);

                setProgress(100, "Ready!");
                Thread.sleep(300);

            } catch (Exception e) {
                System.err.println("Startup task failed: " + e.getMessage());
            }

            SwingUtilities.invokeLater(() -> {
                dispose();
                new LoginPage();
            });
        }).start();
    }
}
