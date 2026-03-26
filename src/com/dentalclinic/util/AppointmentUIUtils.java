package com.dentalclinic.util;

import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.dao.PatientDAO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class AppointmentUIUtils {

    public static void showAppointmentDetails(Component parent, Appointment app, int pID, Runnable refreshCallback) {
        try {
            PatientDAO pDao = new PatientDAO();
            Patient p = pDao.getPatientById(pID);

            JPanel detailPanel = new JPanel();
            detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
            detailPanel.setBackground(Color.WHITE);
            detailPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            Font headerFont = new Font("Arial", Font.BOLD, 16);
            Font dataFont = new Font("Arial", Font.PLAIN, 14);

            JLabel title1 = new JLabel("APPOINTMENT SUMMARY");
            title1.setFont(new Font("Arial", Font.BOLD, 14));
            detailPanel.add(title1);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));

            detailPanel.add(createDetailLabel("Service Type:", app.getServiceType(), dataFont));
            detailPanel.add(createDetailLabel("Date:", app.getAppointmentDate().toString(), dataFont));
            detailPanel.add(createDetailLabel("Time Slot:", app.getAppointmentTime(), dataFont));
            
            JLabel statusLbl = new JLabel("Status: " + app.getStatus().toUpperCase());
            if(app.getStatus().equalsIgnoreCase("Pending")) statusLbl.setForeground(new Color(230, 126, 34));
            else if(app.getStatus().equalsIgnoreCase("Approved")) statusLbl.setForeground(new Color(46, 204, 113));
            else statusLbl.setForeground(Color.RED);
            
            statusLbl.setFont(headerFont);
            detailPanel.add(statusLbl);
            detailPanel.add(Box.createVerticalStrut(15));

            JLabel title2 = new JLabel("PATIENT INFORMATION");
            title2.setFont(new Font("Arial", Font.BOLD, 14));
            detailPanel.add(title2);
            detailPanel.add(Box.createVerticalStrut(5));
            detailPanel.add(new JSeparator());
            detailPanel.add(Box.createVerticalStrut(10));
            
            String fullName = p.getFirstName() + " " + (p.getMiddleName().isEmpty() ? "" : p.getMiddleName() + " ") + p.getLastName();
            detailPanel.add(createDetailLabel("Full Name:", fullName, dataFont));
            detailPanel.add(createDetailLabel("Birthdate:", p.getBirthDate().toString(), dataFont));
            detailPanel.add(createDetailLabel("Age at Booking:", String.valueOf(app.getAgeAtVisit()), dataFont));
            detailPanel.add(createDetailLabel("Contact No:", app.getContactAtVisit(), dataFont));
            detailPanel.add(createDetailLabel("Full Address:", "<html><p style='width:250px'>" + p.getAddress() + "</p></html>", dataFont));

            java.util.List<String> optionsList = new java.util.ArrayList<>();
            if (app.getStatus().equalsIgnoreCase("Approved")) optionsList.add("Download Receipt");
            if (app.getStatus().equalsIgnoreCase("Pending")) optionsList.add("Cancel Request");
            optionsList.add("Close");

            String[] options = optionsList.toArray(new String[0]);
            
            // This replaces your layoutComponent helper
            detailPanel.revalidate();
            detailPanel.repaint();

            int selection = JOptionPane.showOptionDialog(
                parent, detailPanel, "Appointment Summary", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[options.length-1]
            );

            if (selection != -1) {
                String choice = options[selection];
                if (choice.equals("Download Receipt")) {
                    savePanelAsImage(parent, detailPanel, "Receipt_" + app.getAppointmentId());
                } else if (choice.equals("Cancel Request")) {
                    // Logic for cancellation goes here
                    refreshCallback.run(); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JLabel createDetailLabel(String title, String value, Font font) {
        JLabel label = new JLabel("<html><b>" + title + "</b> " + value + "</html>");
        label.setFont(font);
        return label;
    }

    private static void savePanelAsImage(Component parent, JPanel panel, String filename) {
        // Wrap in a frame to ensure sizes are calculated
        JFrame frame = new JFrame();
        frame.add(panel);
        frame.pack();
        
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, panel.getWidth(), panel.getHeight());
        panel.printAll(g2);
        g2.dispose();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filename + ".png"));
        if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(image, "png", fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(parent, "Receipt saved!");
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
}