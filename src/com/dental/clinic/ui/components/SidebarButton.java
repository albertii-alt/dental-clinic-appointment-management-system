package com.dentalclinic.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom sidebar button with modern UI:
 * - Normal: Transparent background, white text
 * - Hover: Dark blue background (#2980b9), white text
 * - Active: Blue background (#3498db), white text + white left border (3px)
 */
public class SidebarButton extends JButton {
    
    private static final Color NORMAL_BG = null; // Transparent
    private static final Color HOVER_BG = new Color(41, 128, 185); // #2980b9
    private static final Color ACTIVE_BG = new Color(52, 152, 219); // #3498db
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ACTIVE_BORDER_COLOR = Color.WHITE;
    private static final int ACTIVE_BORDER_WIDTH = 3;
    
    private boolean isActive = false;
    private boolean isHovering = false;
    private int notificationCount = 0;
    
    public SidebarButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(TEXT_COLOR);
        setFont(new Font("Arial", Font.PLAIN, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.LEFT);
        
        // Add hover listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovering = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovering = false;
                repaint();
            }
        });
    }
    
    /**
     * Set this button as active (selected)
     */
    public void setActive(boolean active) {
        this.isActive = active;
        repaint();
    }
    
    /**
     * Set notification badge count (for Patient dashboard)
     */
    public void setNotificationCount(int count) {
        this.notificationCount = count;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Determine background color
        Color bgColor = null;
        if (isActive) {
            bgColor = ACTIVE_BG;
        } else if (isHovering) {
            bgColor = HOVER_BG;
        }
        
        // Draw background if needed
        if (bgColor != null) {
            g2.setColor(bgColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            // Draw left border for active state
            if (isActive) {
                g2.setColor(ACTIVE_BORDER_COLOR);
                g2.fillRect(0, 0, ACTIVE_BORDER_WIDTH, getHeight());
            }
        }
        
        g2.dispose();
        super.paintComponent(g);
        
        // Draw notification badge if needed
        if (notificationCount > 0) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw red circle
            g2d.setColor(Color.RED);
            int badgeX = getWidth() - 35;
            int badgeY = 10;
            int badgeSize = 18;
            g2d.fillOval(badgeX, badgeY, badgeSize, badgeSize);
            
            // Draw white number
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 11));
            String countStr = String.valueOf(notificationCount);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = badgeX + (badgeSize - fm.stringWidth(countStr)) / 2;
            int textY = badgeY + (badgeSize - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(countStr, textX, textY);
            
            g2d.dispose();
        }
    }
    
    @Override
    public void setBackground(Color bg) {
        // Override to prevent background changes from outside
        // The button manages its own colors
    }
    
    @Override
    public boolean isOpaque() {
        return false;
    }
}