package com.dentalclinic.view.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ErrorDialog extends JDialog {

    private static final Color ERROR_RED = new Color(231, 76, 60);
    private static final Color TEXT_DARK = new Color(44, 62, 80);
    private static final Color TEXT_GRAY = new Color(127, 140, 141);

    public ErrorDialog(Frame parent, String title, String message) {
        super(parent, true);
        setUndecorated(false);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(30, 40, 25, 40));

        // Icon
        JLabel iconLabel = new JLabel(org.kordamp.ikonli.swing.FontIcon.of(
            org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.TIMES_CIRCLE, 52, ERROR_RED));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(iconLabel);
        content.add(Box.createVerticalStrut(14));

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(8));

        // Message
        JLabel msgLabel = new JLabel("<html><div style='text-align:center;width:260px;'>"
            + message.replace("\n", "<br>") + "</div></html>");
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msgLabel.setForeground(TEXT_GRAY);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(msgLabel);
        content.add(Box.createVerticalStrut(22));

        // Button
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapper.setOpaque(false);
        JButton closeBtn = new JButton("Try Again");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        closeBtn.setBackground(ERROR_RED);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setOpaque(true);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setBorder(new EmptyBorder(10, 35, 10, 35));
        closeBtn.addActionListener(e -> dispose());
        btnWrapper.add(closeBtn);
        content.add(btnWrapper);

        setContentPane(content);
        pack();
        setLocationRelativeTo(parent);
    }

    public static void show(Frame parent, String title, String message) {
        new ErrorDialog(parent, title, message).setVisible(true);
    }
}
