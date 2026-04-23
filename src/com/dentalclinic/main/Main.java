package com.dentalclinic.main;

import com.dentalclinic.view.SplashScreen;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // Fix DPI scaling on Windows - prevents UI from being too large/small
        System.setProperty("sun.java2d.uiScale", "1.0");

        // Enable anti-aliased font rendering on all platforms
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Fix selected row text visibility across all platforms
        UIManager.put("Table.selectionForeground", new java.awt.Color(44, 62, 80));
        UIManager.put("Table.selectionBackground", new java.awt.Color(232, 241, 249));
        UIManager.put("List.selectionForeground", new java.awt.Color(44, 62, 80));
        UIManager.put("List.selectionBackground", new java.awt.Color(232, 241, 249));

        // Use cross-platform Metal L&F for consistent rendering without color distortion
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.runStartupAndLaunch();
        });
    }
}
