package com.dentalclinic.view;

import com.dentalclinic.view.components.LogoutDialog;
import com.dentalclinic.view.components.Sidebar;
import com.dentalclinic.view.components.SidebarButton;
import com.dentalclinic.util.UserSession;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import javax.swing.*;
import java.awt.*;
import com.dentalclinic.view.components.WelcomePanel;

public class PatientDashboard extends JFrame {

    private JPanel contentArea;
    private Sidebar sidebar;
    private Timer sessionCheckTimer;
    private SidebarButton notificationBtn;
    
    private int pID;
    private String pfName, pmName, plName, pdob, pAge, pAddress, pContact, pUsername;
    private int notificationCount = 0;

    public PatientDashboard(int pID, String fName, String mName, String lName, String dob, String age, String addr, String phone, String user) {
        
        if (!UserSession.isSessionValid()) {
            new LoginPage();
            dispose();
            return;
        }
        
        this.pID = pID;
        this.pfName = fName;
        this.pmName = mName;
        this.plName = lName;
        this.pdob = dob;
        this.pAge = age;
        this.pAddress = addr;
        this.pContact = phone;
        this.pUsername = user;
        
        setTitle("Dental Clinic - Patient Portal");
        try {
            java.io.InputStream iconStream = getClass().getResourceAsStream("/com/dentalclinic/resources/VantageLogo.png");
            if (iconStream != null) {
                setIconImage(javax.imageio.ImageIO.read(iconStream));
            }
        } catch (Exception ignored) {}
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        sidebar = new Sidebar();
        sidebar.addLogo("Patient Portal", pfName + " " + plName, "Patient", () -> {
            switchPanel(new WelcomePanel(pfName, "Select a category from the sidebar to manage your dental records"));
            sidebar.clearActiveButton();
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Book Appointment", FontAwesomeSolid.CALENDAR_PLUS, () -> {
            try {
                com.dentalclinic.controller.AppointmentController ac = new com.dentalclinic.controller.AppointmentController();
                if (!ac.canPatientBook(pID)) {
                    com.dentalclinic.view.components.ErrorDialog.show(
                        PatientDashboard.this,
                        "Booking Not Allowed",
                        "You already have a Pending or Approved appointment.\n\n" +
                        "You can only book a new appointment once your current one is\n" +
                        "marked as Completed or No Show by the clinic staff."
                    );
                    return;
                }
                switchPanel(new com.dentalclinic.view.patient.BookAppointmentPanel(
                    pID, pfName, pmName, plName, pdob, pAge, pAddress, pContact
                ));
            } catch (Exception ex) {
                switchPanel(new com.dentalclinic.view.patient.BookAppointmentPanel(
                    pID, pfName, pmName, plName, pdob, pAge, pAddress, pContact
                ));
            }
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Today's Schedule", FontAwesomeSolid.CALENDAR_DAY, () -> {
            switchPanel(new com.dentalclinic.view.patient.PatientTodayPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Upcoming Visits", FontAwesomeSolid.CALENDAR_WEEK, () -> {
            switchPanel(new com.dentalclinic.view.patient.PatientUpcomingPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("My Appointment Requests", FontAwesomeSolid.CLOCK, () -> {
            switchPanel(new com.dentalclinic.view.patient.ViewAppointmentsPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Medical History", FontAwesomeSolid.NOTES_MEDICAL, () -> {
            switchPanel(new com.dentalclinic.view.patient.PatientHistoryPanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Cancelled List", FontAwesomeSolid.BAN, () -> {
            switchPanel(new com.dentalclinic.view.patient.PatientCancelledPanel(pID));
            UserSession.updateActivity();
        });
        
        notificationBtn = sidebar.addButton("Notifications", FontAwesomeSolid.BELL, () -> {
            switchPanel(new com.dentalclinic.view.patient.PatientNotificationPanel(pID, this));
            UserSession.updateActivity();
        });
        
        sidebar.addButton("Profile", FontAwesomeSolid.USER, () -> {
            switchPanel(new com.dentalclinic.view.patient.PatientProfilePanel(pID));
            UserSession.updateActivity();
        });
        
        sidebar.addSpecialButton("Logout", 600, new Color(192, 57, 43), () -> {
            boolean confirm = LogoutDialog.show(this);
            if (confirm) {
                if (sessionCheckTimer != null) sessionCheckTimer.stop();
                UserSession.logout();
                new LoginPage();
                dispose();
            }
        });
        
        contentArea = new JPanel(new GridBagLayout());
        contentArea.setBackground(new Color(236, 240, 241));
        
        add(sidebar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);
        
        startSessionMonitor();
        switchPanel(new WelcomePanel(pfName, "Select a category from the sidebar to manage your dental records"));
        refreshNotificationBadge();
        
        setVisible(true);
    }
    
    private void startSessionMonitor() {
        sessionCheckTimer = new Timer(10000, e -> {
            if (!UserSession.isSessionValid()) {
                sessionCheckTimer.stop();
                com.dentalclinic.view.components.ErrorDialog.show(this, 
                    "Session Expired", 
                    "Your session has expired due to inactivity.\nPlease login again.");
                UserSession.logout();
                new LoginPage();
                dispose();
            }
        });
        sessionCheckTimer.start();
    }
    
    
    private void switchPanel(JPanel newPanel) {
        contentArea.removeAll();
        contentArea.setLayout(new BorderLayout());
        contentArea.add(newPanel, BorderLayout.CENTER);
        contentArea.revalidate();
        contentArea.repaint();
        UserSession.updateActivity();
    }

    public void refreshNotificationBadge() {
        try {
            com.dentalclinic.dao.AppointmentDAO dao = new com.dentalclinic.dao.AppointmentDAO();
            this.notificationCount = dao.getUnreadNotificationCount(pID);
            notificationBtn.setNotificationCount(notificationCount);
            notificationBtn.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
