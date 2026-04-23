package com.dentalclinic.main;

import com.dentalclinic.view.SplashScreen;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // Enable anti-aliased font rendering on all platforms
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.renderer.aa", "true");

        // Fix selected row text visibility across all platforms
        UIManager.put("Table.selectionForeground", new java.awt.Color(44, 62, 80));
        UIManager.put("Table.selectionBackground", new java.awt.Color(232, 241, 249));
        UIManager.put("List.selectionForeground", new java.awt.Color(44, 62, 80));
        UIManager.put("List.selectionBackground", new java.awt.Color(232, 241, 249));

        // Use cross-platform Metal L&F for consistent rendering without color distortion
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            // Override Metal default fonts with Segoe UI on Windows, SansSerif elsewhere
            String os = System.getProperty("os.name", "").toLowerCase();
            String fontName = os.contains("win") ? "Segoe UI" : "SansSerif";
            java.awt.Font baseFont = new java.awt.Font(fontName, java.awt.Font.PLAIN, 13);
            UIManager.put("Button.font", baseFont);
            UIManager.put("Label.font", baseFont);
            UIManager.put("TextField.font", baseFont);
            UIManager.put("TextArea.font", baseFont);
            UIManager.put("ComboBox.font", baseFont);
            UIManager.put("Table.font", baseFont);
            UIManager.put("TableHeader.font", baseFont.deriveFont(java.awt.Font.BOLD));
            UIManager.put("List.font", baseFont);
            UIManager.put("Panel.font", baseFont);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.runStartupAndLaunch();
        });
    }
}
