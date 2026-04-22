package com.dentalclinic.view.components;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Sidebar extends JPanel {
    
    // Gradient Colors: Updated to match RegisterPatientForm/LoginPage
    private final Color SIDEBAR_START = new Color(20, 30, 48); 
    private final Color SIDEBAR_END = new Color(36, 59, 85);   
    private final Color SUBMENU_BG = new Color(25, 35, 50); // Slightly lighter than start for contrast
    
    private List<SidebarButton> allButtons = new ArrayList<>();
    private List<JComponent> allComponents = new ArrayList<>();
    private SidebarButton currentActiveButton;
    private JPanel subMenuPanel;
    private int nextY = 100;
    private List<JComponent> shiftableComponents = new ArrayList<>();
    private boolean isSubMenuOpen = false;
    
    public Sidebar() {
        setLayout(null);
        // setOpaque(false) tells Swing we will handle the background painting ourselves
        setOpaque(false);
        setPreferredSize(new Dimension(250, 700));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        int w = getWidth();
        int h = getHeight();
        
        // Updated to a diagonal gradient (0,0 to w,h)
        GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_START, w, h, SIDEBAR_END);
        
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);
        
        g2d.dispose();
        super.paintComponent(g);
    }
    
    public void addLogo(String title, Runnable onClick) {
        addLogo(title, null, null, onClick);
    }

    public void addLogo(String title, String userName, String userRole, Runnable onClick) {
        JPanel logoPanel = new JPanel();
        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
        logoPanel.setOpaque(false);
        logoPanel.setBounds(0, 10, 250, 90);
        logoPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (onClick != null) onClick.run();
            }
        });

        // --- Clinic name row: logo icon + name ---
        JPanel clinicRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        clinicRow.setOpaque(false);

        // Logo image
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/com/dentalclinic/resources/VantageLogo.png");
            if (is != null) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
                java.awt.Image scaled = img.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                JLabel logoImg = new JLabel(new javax.swing.ImageIcon(scaled));
                clinicRow.add(logoImg);
            }
        } catch (Exception ignored) {}

        JLabel clinicName = new JLabel("Vantage Dental");
        clinicName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        clinicName.setForeground(Color.WHITE);
        clinicRow.add(clinicName);

        logoPanel.add(Box.createVerticalStrut(8));
        logoPanel.add(clinicRow);

        // --- Divider ---
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 50));
        sep.setMaximumSize(new Dimension(210, 1));
        logoPanel.add(Box.createVerticalStrut(6));
        logoPanel.add(sep);
        logoPanel.add(Box.createVerticalStrut(6));

        // --- User info row ---
        if (userName != null && userRole != null) {
            JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            userRow.setOpaque(false);

            // User icon
            JLabel userIcon = new JLabel();
            userIcon.setIcon(org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_CIRCLE, 18, Color.WHITE));
            userRow.add(userIcon);

            // Name + role badge stacked
            JPanel nameRolePanel = new JPanel();
            nameRolePanel.setLayout(new BoxLayout(nameRolePanel, BoxLayout.Y_AXIS));
            nameRolePanel.setOpaque(false);

            JLabel nameLabel = new JLabel(userName);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            nameLabel.setForeground(Color.WHITE);

            JLabel roleLabel = new JLabel(userRole.toUpperCase());
            roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            roleLabel.setForeground(new Color(174, 214, 241));

            nameRolePanel.add(nameLabel);
            nameRolePanel.add(roleLabel);
            userRow.add(nameRolePanel);
            logoPanel.add(userRow);
        } else {
            // Fallback: just show the title
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            titleLabel.setForeground(new Color(174, 214, 241));
            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            titleRow.setOpaque(false);
            titleRow.add(titleLabel);
            logoPanel.add(titleRow);
        }

        add(logoPanel);
        allComponents.add(logoPanel);
        nextY = 110;
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
        label.setForeground(new Color(189, 195, 199)); // Brighter silver (#BDC3C7)
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
        btn.setIcon(org.kordamp.ikonli.swing.FontIcon.of(FontAwesomeSolid.SIGN_OUT_ALT, 14, Color.WHITE));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(10);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor.darker().darker());
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
        });
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