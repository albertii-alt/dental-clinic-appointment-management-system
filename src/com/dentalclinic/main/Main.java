package com.dentalclinic.main;

import com.dentalclinic.controller.AppointmentController;
import com.dentalclinic.view.LoginPage;

public class Main {

    public static void main(String[] args) {
        new Thread(() -> {
            try {
                AppointmentController appointmentController = new AppointmentController();
                int tomorrowSent = appointmentController.sendAllRemindersForTomorrow();
                if (tomorrowSent > 0) {
                    System.out.println("Sent " + tomorrowSent + " appointment reminders for tomorrow");
                }

                int todaySent = appointmentController.sendAllDayOfReminders();
                if (todaySent > 0) {
                    System.out.println("Sent " + todaySent + " day-of appointment reminders");
                }
            } catch (Exception e) {
                System.err.println("Failed to send reminders: " + e.getMessage());
            }
        }).start();

        new LoginPage();
    }
}
