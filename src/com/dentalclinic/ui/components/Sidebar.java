package com.dentalclinic.ui.components;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Sidebar extends JPanel {
    
    private final Color SIDEBAR_BG = new Color(44, 62, 80);
    private final Color SUBMENU_BG = new Color(34, 49, 63);
    
    private List<SidebarButton> allButtons = new ArrayList<>();
    private List<JComponent> allComponents = new ArrayList<>();
    private SidebarButton currentActiveButton;
    private JPanel subMenuPanel;
    private int nextY = 100;
    private List<JComponent> shiftableComponents = new ArrayList<>();
    private boolean isSubMenuOpen = false;
    
    public Sidebar() {
        setLayout(null);
        setBackground(SIDEBAR_BG);
        setPreferredSize(new Dimension(250, 700));
    }
    
    public void addLogo(String title, Runnable onClick) {
        JLabel logoLabel = new JLabel(title);
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
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
    
    public SidebarButton addButton(String text, Ikon icon, Runnable onClick) {
        SidebarButton btn = new SidebarButton(text);
        btn.setIcon(icon);
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
    
    public SidebarButton addButtonAt(String text, Ikon icon, int y, Runnable onClick) {
        SidebarButton btn = new SidebarButton(text);
        btn.setIcon(icon);
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
    
    public void addLabel(String text, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(171, 183, 183));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setBounds(25, y, 150, 20);
        add(label);
        allComponents.add(label);
    }
    
    public JPanel createSubMenu(int y, int height) {
        subMenuPanel = new JPanel(null);
        subMenuPanel.setBackground(SUBMENU_BG);
        subMenuPanel.setBounds(20, y, 210, height);
        subMenuPanel.setVisible(false);
        add(subMenuPanel);
        allComponents.add(subMenuPanel);
        return subMenuPanel;
    }
    
    public SidebarButton addSubButton(JPanel subMenu, String text, Ikon icon, int y, Runnable onClick) {
        SidebarButton btn = new SidebarButton(text);
        btn.setIcon(icon);
        btn.setBounds(10, y, 190, 30);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.addActionListener(e -> {
            setActiveButton(btn);
            if (onClick != null) onClick.run();
        });
        subMenu.add(btn);
        allButtons.add(btn);
        allComponents.add(btn);
        return btn;
    }
    
    public void registerShiftableComponent(JComponent comp) {
        shiftableComponents.add(comp);
    }
    
    public void toggleSubMenu(List<JComponent> componentsToShift, int shiftAmount) {
        isSubMenuOpen = !isSubMenuOpen;
        subMenuPanel.setVisible(isSubMenuOpen);
        
        int shift = isSubMenuOpen ? shiftAmount : -shiftAmount;
        
        for (JComponent comp : componentsToShift) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        for (JComponent comp : shiftableComponents) {
            comp.setLocation(comp.getX(), comp.getY() + shift);
        }
        
        repaint();
    }
    
    public JPanel getSubMenuPanel() {
        return subMenuPanel;
    }
    
    public boolean isSubMenuOpen() {
        return isSubMenuOpen;
    }
    
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
    
    public void clearActiveButton() {
        if (currentActiveButton != null) {
            currentActiveButton.setActive(false);
            currentActiveButton = null;
        }
        repaint();
    }
    
    public SidebarButton getButtonByText(String text) {
        for (SidebarButton btn : allButtons) {
            if (btn.getText().equals(text)) {
                return btn;
            }
        }
        return null;
    }
    
    public void addSpecialButton(String text, int y, Color bgColor, Runnable onClick) {
        JButton btn = new JButton(text);
        btn.setBounds(20, y, 210, 40);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            if (onClick != null) onClick.run();
        });
        add(btn);
        allComponents.add(btn);
    }
    
    public int getCurrentY() {
        return nextY;
    }
    
    public void setCurrentY(int y) {
        this.nextY = y;
    }
}