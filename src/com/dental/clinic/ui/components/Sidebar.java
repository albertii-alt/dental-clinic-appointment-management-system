package com.dentalclinic.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable sidebar component for all dashboards
 */
public class Sidebar extends JPanel {
    
    private final Color SIDEBAR_BG = new Color(44, 62, 80);
    private final Color SUBMENU_BG = new Color(34, 49, 63);
    
    private List<SidebarButton> allButtons = new ArrayList<>();
    private List<JComponent> allComponents = new ArrayList<>();
    private SidebarButton currentActiveButton;
    private JPanel subMenuPanel;
    private int nextY = 100;
    private int subMenuStartY = 0;
    private int subMenuHeight = 0;
    private List<JComponent> shiftableComponents = new ArrayList<>();
    private boolean isSubMenuOpen = false;
    
    public Sidebar() {
        setLayout(null);
        setBackground(SIDEBAR_BG);
        setPreferredSize(new Dimension(250, 700));
    }
    
    /**
     * Add logo/title to sidebar
     */
    public void addLogo(String title, Runnable onClick) {
        JLabel logoLabel = new JLabel(title);
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Arial", Font.BOLD, 22));
        logoLabel.setBounds(50, 30, 150, 30);
        logoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });
        add(logoLabel);
        allComponents.add(logoLabel);
    }
    
    /**
     * Add a main sidebar button
     */
    public SidebarButton addButton(String text, Runnable onClick) {
        SidebarButton btn = new SidebarButton(text);
        btn.setBounds(20, nextY, 210, 40);
        btn.addActionListener(e -> {
            setActiveButton(btn);
            if (onClick != null) onClick.run();
        });
        add(btn);
        allButtons.add(btn);
        allComponents.add(btn);
        nextY += 50;
        return btn;
    }
    
    /**
     * Add a button with custom Y position
     */
    public SidebarButton addButtonAt(String text, int y, Runnable onClick) {
        SidebarButton btn = new SidebarButton(text);
        btn.setBounds(20, y, 210, 40);
        btn.addActionListener(e -> {
            setActiveButton(btn);
            if (onClick != null) onClick.run();
        });
        add(btn);
        allButtons.add(btn);
        allComponents.add(btn);
        return btn;
    }
    
    /**
     * Add a separator/label
     */
    public void addLabel(String text, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(171, 183, 183));
        label.setFont(new Font("Arial", Font.PLAIN, 11));
        label.setBounds(25, y, 150, 20);
        add(label);
        allComponents.add(label);
    }
    
    /**
     * Create a submenu panel (must be called before adding buttons below it)
     */
    public JPanel createSubMenu(int y, int height) {
        this.subMenuStartY = y;
        this.subMenuHeight = height;
        subMenuPanel = new JPanel(null);
        subMenuPanel.setBackground(SUBMENU_BG);
        subMenuPanel.setBounds(20, y, 210, height);
        subMenuPanel.setVisible(false);
        add(subMenuPanel);
        allComponents.add(subMenuPanel);
        return subMenuPanel;
    }
    
    /**
     * Add a submenu button
     */
    public SidebarButton addSubButton(JPanel subMenu, String text, int y, Runnable onClick) {
        SidebarButton btn = new SidebarButton(text);
        btn.setBounds(10, y, 190, 30);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.addActionListener(e -> {
            setActiveButton(btn);
            if (onClick != null) onClick.run();
        });
        subMenu.add(btn);
        allButtons.add(btn);
        allComponents.add(btn);
        return btn;
    }
    
    /**
     * Register a component that should shift when submenu opens
     */
    public void registerShiftableComponent(JComponent comp) {
        shiftableComponents.add(comp);
    }
    
    /**
     * Toggle submenu and shift buttons below it
     */
    public void toggleSubMenu(List<JComponent> componentsToShift, int shiftAmount) {
        isSubMenuOpen = !isSubMenuOpen;
        subMenuPanel.setVisible(isSubMenuOpen);
        
        int shift = isSubMenuOpen ? shiftAmount : -shiftAmount;
        
        for (JComponent comp : componentsToShift) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        // Also shift registered components
        for (JComponent comp : shiftableComponents) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        repaint();
    }
    
    /**
     * Get the submenu panel
     */
    public JPanel getSubMenuPanel() {
        return subMenuPanel;
    }
    
    /**
     * Check if submenu is open
     */
    public boolean isSubMenuOpen() {
        return isSubMenuOpen;
    }
    
    /**
     * Set the active button (clears previous active state)
     */
    public void setActiveButton(SidebarButton button) {
        if (currentActiveButton != null) {
            currentActiveButton.setActive(false);
        }
        currentActiveButton = button;
        if (currentActiveButton != null) {
            currentActiveButton.setActive(true);
        }
        repaint();
    }
    
    /**
     * Clear active button (for logo click)
     */
    public void clearActiveButton() {
        if (currentActiveButton != null) {
            currentActiveButton.setActive(false);
            currentActiveButton = null;
        }
        repaint();
    }
    
    /**
     * Get button by text (for notification badge updates)
     */
    public SidebarButton getButtonByText(String text) {
        for (SidebarButton btn : allButtons) {
            if (btn.getText().equals(text)) {
                return btn;
            }
        }
        return null;
    }
    
    /**
     * Add a special button (like Logout with red color)
     */
    public void addSpecialButton(String text, int y, Color bgColor, Runnable onClick) {
        JButton btn = new JButton(text);
        btn.setBounds(20, y, 210, 40);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.PLAIN, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (onClick != null) onClick.run();
        });
        add(btn);
        allComponents.add(btn);
    }
    
    /**
     * Get the current Y position (for adding buttons)
     */
    public int getCurrentY() {
        return nextY;
    }
    
    /**
     * Set the current Y position
     */
    public void setCurrentY(int y) {
        this.nextY = y;
    }
}