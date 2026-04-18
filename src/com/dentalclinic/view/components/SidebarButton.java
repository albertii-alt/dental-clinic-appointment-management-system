package com.dentalclinic.view.components;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SidebarButton extends JButton {
    
    private static final Color NORMAL_BG = null;
    private static final Color HOVER_BG = new Color(41, 128, 185);
    private static final Color ACTIVE_BG = new Color(52, 152, 219);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color ACTIVE_BORDER_COLOR = Color.WHITE;
    private static final int ACTIVE_BORDER_WIDTH = 3;
    
    private boolean isActive = false;
    private boolean isHovering = false;
    private int notificationCount = 0;
    private Ikon icon;
    
    public SidebarButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(TEXT_COLOR);
        setFont(new Font("Segoe UI", Font.PLAIN, 13));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(12);
        
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
     * Set icon using Ikonli
     */
    public void setIcon(Ikon ikon) {
        this.icon = ikon;
        updateIcon();
    }
    
    private void updateIcon() {
        if (icon != null) {
            setIcon(FontIcon.of(icon, 16, Color.WHITE));
        }
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
        repaint();
    }
    
    public void setNotificationCount(int count) {
        this.notificationCount = count;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Color bgColor = null;
        if (isActive) {
            bgColor = ACTIVE_BG;
        } else if (isHovering) {
            bgColor = HOVER_BG;
        }
        
        if (bgColor != null) {
            g2.setColor(bgColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            if (isActive) {
                g2.setColor(ACTIVE_BORDER_COLOR);
                g2.fillRect(0, 0, ACTIVE_BORDER_WIDTH, getHeight());
            }
        }
        
        g2.dispose();
        super.paintComponent(g);
        
        // Draw notification badge
        if (notificationCount > 0) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(Color.RED);
            int badgeX = getWidth() - 35;
            int badgeY = 10;
            int badgeSize = 18;
            g2d.fillOval(badgeX, badgeY, badgeSize, badgeSize);
            
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
}