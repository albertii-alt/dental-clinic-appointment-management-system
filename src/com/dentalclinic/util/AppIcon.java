package com.dentalclinic.util;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.awt.Image;
import java.io.InputStream;

public class AppIcon {

    private static Image icon;

    private static Image load() {
        if (icon != null) return icon;
        try (InputStream is = AppIcon.class.getResourceAsStream("/com/dentalclinic/resources/VantageLogo.png")) {
            if (is != null) icon = ImageIO.read(is);
        } catch (Exception ignored) {}
        return icon;
    }

    public static void apply(JFrame frame) {
        Image img = load();
        if (img != null) frame.setIconImage(img);
    }
}
