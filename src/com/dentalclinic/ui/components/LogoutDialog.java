package com.dentalclinic.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class LogoutDialog extends JDialog {

    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_COLOR = new Color(230, 230, 230);
    private boolean confirmed = false;

    public LogoutDialog(Frame parent) {
        super(parent, true);
        setUndecorated(true);
        setSize(400, 280);
        setLocationRelativeTo(parent);
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        add(content);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        // 1. Icon (Question Mark or Power Icon)
        JLabel iconLabel = new JLabel("?");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 60));
        iconLabel.setForeground(PRIMARY_BLUE);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 0, 10, 0);
        content.add(iconLabel, gbc);

        // 2. Title
        JLabel titleLabel = new JLabel("Confirm Logout");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 40, 5, 40);
        content.add(titleLabel, gbc);

        // 3. Message
        JLabel msgLabel = new JLabel("Are you sure you want to end your session?");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setForeground(TEXT_GRAY);
        msgLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 40, 30, 40);
        content.add(msgLabel, gbc);

        // 4. Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, Color.WHITE, TEXT_DARK, true);
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JButton logoutBtn = new JButton("Logout");
        styleButton(logoutBtn, PRIMARY_BLUE, Color.WHITE, false);
        logoutBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(logoutBtn);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 40, 30, 40);
        content.add(btnPanel, gbc);
    }

    private void styleButton(JButton btn, Color bg, Color fg, boolean hasBorder) {
        btn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setPreferredSize(new Dimension(120, 45));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (hasBorder) {
            btn.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        } else {
            btn.setBorder(null);
        }

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bg == Color.WHITE ? new Color(245, 245, 245) : bg.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public static boolean show(Frame parent) {
        LogoutDialog dialog = new LogoutDialog(parent);
        dialog.setVisible(true);
        return dialog.isConfirmed();
    }
}