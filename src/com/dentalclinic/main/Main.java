package com.dentalclinic.main;

import com.dentalclinic.view.SplashScreen;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        // Fix DPI scaling on Windows - prevents UI from being too large/small
        System.setProperty("sun.java2d.uiScale", "1.0");

        // Set Nimbus Look and Feel for consistent cross-platform rendering
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Enable anti-aliased font rendering on all platforms
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.runStartupAndLaunch();
        });
    }
}
