package com.dental.clinic.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ErrorDialog extends JDialog {

    private final Color ERROR_RED = new Color(231, 76, 60);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);

    public ErrorDialog(Frame parent, String title, String message) {
        super(parent, true);
        setUndecorated(true);
        setSize(500, 500);
        setLocationRelativeTo(parent);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createLineBorder(new Color(245, 245, 245), 1));
        add(content);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Error Icon (X)
        JLabel iconLabel = new JLabel("✕");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 60));
        iconLabel.setForeground(ERROR_RED);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        content.add(iconLabel, gbc);

        // 2. Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 40, 5, 40);
        content.add(titleLabel, gbc);

        // 3. Message
        JLabel msgLabel = new JLabel("<html><center>" + message + "</center></html>");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setForeground(TEXT_GRAY);
        msgLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 40, 25, 40);
        content.add(msgLabel, gbc);

        // 4. Close Button
        JButton closeBtn = new JButton("Try Again");
        closeBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        closeBtn.setBackground(ERROR_RED);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(0, 45));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(null);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 80, 20, 80);
        content.add(closeBtn, gbc);
    }

    public static void show(Frame parent, String title, String message) {
        new ErrorDialog(parent, title, message).setVisible(true);
    }
}