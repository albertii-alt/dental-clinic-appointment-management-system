package com.dentalclinic.util;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailUtil {
    
    // Email configuration
    private static String FROM_EMAIL;
    private static String FROM_PASSWORD;
    private static String SMTP_HOST;
    private static String SMTP_PORT;
    private static boolean configLoaded = false;
    
    // Load email config from file
    static {
        loadEmailConfig();
    }
    
    private static void loadEmailConfig() {
        String userHome = System.getProperty("user.home");
        String configPath = userHome + File.separator + ".dental_clinic" + File.separator + "db.properties";
        
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(configPath)) {
            props.load(input);
            
            FROM_EMAIL = props.getProperty("email.user");
            FROM_PASSWORD = props.getProperty("email.password");
            SMTP_HOST = props.getProperty("email.smtp.host", "smtp.gmail.com");
            SMTP_PORT = props.getProperty("email.smtp.port", "587");
            
            if (FROM_EMAIL != null && !FROM_EMAIL.isEmpty()) {
                configLoaded = true;
                System.out.println("Email configuration loaded successfully");
            }
        } catch (IOException e) {
            System.err.println("Email config not found. Email notifications disabled.");
        }
    }
    
    /**
     * Send email notification
     */
    public static boolean sendEmail(String toEmail, String subject, String body) {
        if (!configLoaded) {
            System.err.println("Email not configured. Notification not sent.");
            return false;
        }
        
        try {
            // SMTP properties
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            
            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                }
            });
            
            // Create email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "Vantage Dental Clinic"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            
            // Send email
            Transport.send(message);
            System.out.println("Email sent to: " + toEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Email error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send welcome email after patient registration
     */
    public static void sendWelcomeEmail(String patientName, String email, String username, String password) {
        String subject = "Welcome to Vantage Dental Clinic! 🦷";
        String body = "Dear " + patientName + ",\n\n" +
                      "Thank you for registering with Vantage Dental Clinic!\n\n" +
                      "Your account has been created successfully.\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "LOGIN CREDENTIALS\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Username: " + username + "\n" +
                      "Password: " + password + "\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                      "You can now:\n" +
                      "✓ Book appointments online\n" +
                      "✓ View your medical history\n" +
                      "✓ Receive appointment reminders\n\n" +
                      "To login, open the Dental Clinic application.\n\n" +
                      "Best regards,\n" +
                      "Vantage Dental Clinic Team\n" +
                      "http://localhost/phpmyadmin";
        
        sendEmail(email, subject, body);
    }
    
    /**
     * Send appointment confirmation email
     */
    public static void sendAppointmentConfirmation(String patientName, String email, 
                                                     String serviceType, String date, String time, int appointmentId) {
        String subject = "Appointment Confirmation - Vantage Dental Clinic";
        String body = "Dear " + patientName + ",\n\n" +
                      "Your appointment has been CONFIRMED!\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "APPOINTMENT DETAILS\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Reference ID: #" + appointmentId + "\n" +
                      "Service: " + serviceType + "\n" +
                      "Date: " + date + "\n" +
                      "Time: " + time + "\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                      "Please arrive 10 minutes before your scheduled time.\n\n" +
                      "To cancel or reschedule, please contact the clinic.\n\n" +
                      "Thank you for choosing Vantage Dental Clinic!\n\n" +
                      "Best regards,\n" +
                      "Vantage Dental Clinic Team";
        
        sendEmail(email, subject, body);
    }
    
    /**
     * Send appointment reminder (day before)
     */
    public static void sendAppointmentReminder(String patientName, String email, 
                                                String serviceType, String date, String time) {
        String subject = "Appointment Reminder - Tomorrow at Vantage Dental Clinic";
        String body = "Dear " + patientName + ",\n\n" +
                      "This is a reminder about your appointment TOMORROW.\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Service: " + serviceType + "\n" +
                      "Date: " + date + "\n" +
                      "Time: " + time + "\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                      "Please bring any relevant medical records.\n\n" +
                      "We look forward to seeing you!\n\n" +
                      "Best regards,\n" +
                      "Vantage Dental Clinic Team";
        
        sendEmail(email, subject, body);
    }
    
    /**
     * Send appointment cancellation notification
     */
    public static void sendCancellationNotification(String patientName, String email, 
                                                     String serviceType, String date, String time) {
        String subject = "Appointment Cancelled - Vantage Dental Clinic";
        String body = "Dear " + patientName + ",\n\n" +
                      "We regret to inform you that your appointment has been CANCELLED.\n\n" +
                      "Cancelled Appointment:\n" +
                      "Service: " + serviceType + "\n" +
                      "Date: " + date + "\n" +
                      "Time: " + time + "\n\n" +
                      "Please contact the clinic to reschedule.\n\n" +
                      "We apologize for any inconvenience.\n\n" +
                      "Best regards,\n" +
                      "Vantage Dental Clinic Team";
        
        sendEmail(email, subject, body);
    }
    
    /**
     * Check if email is configured
     */
    public static boolean isConfigured() {
        return configLoaded;
    }
    /**
 * Send appointment declined notification
 */
    public static void sendDeclinedNotification(String patientName, String email, 
                                                  String serviceType, String date, String time, String reason) {
        String subject = "Appointment Declined - Vantage Dental Clinic";
        String body = "Dear " + patientName + ",\n\n" +
                      "We regret to inform you that your appointment request has been DECLINED.\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "APPOINTMENT DETAILS\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Service: " + serviceType + "\n" +
                      "Date: " + date + "\n" +
                      "Time: " + time + "\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n";

        if (reason != null && !reason.isEmpty()) {
            body += "Reason: " + reason + "\n\n";
        }

        body += "Please contact the clinic to schedule a different time.\n\n" +
                "We apologize for any inconvenience.\n\n" +
                "Best regards,\n" +
                "Vantage Dental Clinic Team";

        sendEmail(email, subject, body);
    }

    /**
     * Send appointment rescheduled notification
     */
    public static void sendRescheduledNotification(String patientName, String email, 
                                                    String serviceType, String oldDate, String oldTime, 
                                                    String newDate, String newTime) {
        String subject = "Appointment Rescheduled - Vantage Dental Clinic";
        String body = "Dear " + patientName + ",\n\n" +
                      "Your appointment has been RESCHEDULED.\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "PREVIOUS APPOINTMENT\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Service: " + serviceType + "\n" +
                      "Date: " + oldDate + "\n" +
                      "Time: " + oldTime + "\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "NEW APPOINTMENT\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Service: " + serviceType + "\n" +
                      "Date: " + newDate + "\n" +
                      "Time: " + newTime + "\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                      "If this new time doesn't work for you, please contact the clinic.\n\n" +
                      "Best regards,\n" +
                      "Vantage Dental Clinic Team";

        sendEmail(email, subject, body);
    }
    public static void testConfig() {
        System.out.println("Email configured: " + configLoaded);
        System.out.println("From Email: " + FROM_EMAIL);
        System.out.println("SMTP Host: " + SMTP_HOST);
        System.out.println("SMTP Port: " + SMTP_PORT);
    }
}