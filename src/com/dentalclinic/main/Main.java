package com.dentalclinic.main;

import com.dentalclinic.view.SplashScreen;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SplashScreen splash = new SplashScreen();
            splash.runStartupAndLaunch();
        });
    }
}
