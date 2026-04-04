package com.dentalclinic.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class SuccessDialog extends JDialog {

    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color SUCCESS_GREEN = new Color(39, 174, 96);

    public SuccessDialog(Frame parent, String title, String message) {
        super(parent, true);
        setUndecorated(true); // Removes standard title bar for a cleaner look
        setSize(400, 320);
        setLocationRelativeTo(parent);

        // Rounded corners for the dialog
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        add(content);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 40, 10, 40);

        // 1. Success Icon (Using a large Unicode checkmark or a simple drawing)
        JLabel iconLabel = new JLabel("✓");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 70));
        iconLabel.setForeground(SUCCESS_GREEN);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 0;
        content.add(iconLabel, gbc);

        // 2. Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
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
        gbc.insets = new Insets(0, 40, 30, 40);
        content.add(msgLabel, gbc);

        // 4. Action Button
        JButton actionBtn = new JButton("Get Started");
        styleButton(actionBtn);
        actionBtn.addActionListener(e -> dispose());
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 80, 20, 80);
        content.add(actionBtn, gbc);
    }

    private void styleButton(JButton btn) {
        btn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        btn.setBackground(PRIMARY_BLUE);
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(0, 45));
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Simple hover
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_BLUE.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(PRIMARY_BLUE); }
        });
    }

    // Static helper to call it easily
    public static void show(Frame parent, String title, String message) {
        SuccessDialog dialog = new SuccessDialog(parent, title, message);
        dialog.setVisible(true);
    }
}