package com.dentalclinic.ui.components;

import javax.swing.*;
import java.awt.*;

public class WelcomePanel extends JPanel {

    public WelcomePanel(String name, String subtitle) {
        // Use the same background color from your dashboards
        setBackground(new Color(236, 240, 241));
        setLayout(new GridBagLayout());

        // Main Welcome Message
        JLabel welcomeMsg = new JLabel("Welcome, " + name);
        welcomeMsg.setFont(new Font("Arial", Font.BOLD, 28));
        welcomeMsg.setForeground(new Color(52, 73, 94));

        // Subtitle Message
        JLabel subMsg = new JLabel(subtitle);
        subMsg.setFont(new Font("Arial", Font.PLAIN, 18));
        subMsg.setForeground(Color.GRAY);

        // Layout Constraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0); // 10px spacing between lines
        add(welcomeMsg, gbc);

        gbc.gridy = 1;
        add(subMsg, gbc);
    }
}   