package com.dentalclinic.util;

import com.dentalclinic.service.LogService;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailUtil {
    
    // Email configuration
    private static String FROM_EMAIL;
    private static String FROM_PASSWORD;
    private static String SMTP_HOST;
    private static String SMTP_PORT;
    private static boolean configLoaded = false;
    
    // Background email sender thread pool (max 5 concurrent emails)
    private static final ExecutorService emailExecutor = Executors.newFixedThreadPool(5);
    
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
                logEmailEvent("INFO", "Email system initialized from: " + FROM_EMAIL);
            }
        } catch (IOException e) {
            System.err.println("Email config not found. Email notifications disabled.");
            logEmailEvent("ERROR", "Email config not found");
        }
    }
    
    /**
     * Log email events for system logs
     */
    private static void logEmailEvent(String level, String message) {
        try {
            LogService.logSystemEvent(level, "EmailUtil", message);
        } catch (Exception e) {
            System.out.println("[EMAIL " + level + "] " + message);
        }
    }
    
    /**
     * Add to audit trails
     */
    private static void addToAuditTrail(int actorId, String actorRole, String action, String details) {
        if (actorId > 0) {
            try {
                LogService logService = new LogService();
                logService.record(actorId, actorRole, action, details);
            } catch (Exception e) {
                System.err.println("Audit log failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Send email notification (blocking - waits for email to send)
     */
    public static boolean sendEmail(String toEmail, String subject, String body) {
        if (!configLoaded) {
            System.err.println("Email not configured. Notification not sent.");
            logEmailEvent("WARNING", "Attempted to send email but email not configured");
            return false;
        }
        
        String maskedEmail = maskEmail(toEmail);
        
        logEmailEvent("INFO", "Attempting to send email to: " + maskedEmail + " | Subject: " + subject);
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "Vantage Dental Clinic"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            System.out.println("Email sent to: " + maskedEmail);
            logEmailEvent("INFO", "SUCCESS - Email sent to: " + maskedEmail);
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Failed to send email to: " + maskedEmail + " | Error: " + e.getMessage());
            logEmailEvent("ERROR", "FAILED - Email to " + maskedEmail + " | Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Email error to: " + maskedEmail + " | Error: " + e.getMessage());
            logEmailEvent("ERROR", "FAILED - Email to " + maskedEmail + " | Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send email notification with audit trail (blocking)
     */
    public static boolean sendEmail(String toEmail, String subject, String body, int actorId, String actorRole, String actionDescription) {
        if (!configLoaded) {
            System.err.println("Email not configured. Notification not sent.");
            logEmailEvent("WARNING", "Attempted to send email but email not configured");
            return false;
        }
        
        String maskedEmail = maskEmail(toEmail);
        
        logEmailEvent("INFO", "Attempting to send email to: " + maskedEmail + " | Subject: " + subject);
        
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "Vantage Dental Clinic"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            System.out.println("Email sent to: " + maskedEmail);
            logEmailEvent("INFO", "SUCCESS - Email sent to: " + maskedEmail);
            
            // Add to Audit Trails
            if (actorId > 0 && actionDescription != null) {
                addToAuditTrail(actorId, actorRole, "Email Sent", actionDescription + " | To: " + maskedEmail);
            }
            
            return true;
            
        } catch (MessagingException e) {
            System.err.println("Failed to send email to: " + maskedEmail + " | Error: " + e.getMessage());
            logEmailEvent("ERROR", "FAILED - Email to " + maskedEmail + " | Error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Email error to: " + maskedEmail + " | Error: " + e.getMessage());
            logEmailEvent("ERROR", "FAILED - Email to " + maskedEmail + " | Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Send email in background (non-blocking) - Recommended for better performance
     */
    public static void sendEmailAsync(String toEmail, String subject, String body) {
        emailExecutor.submit(() -> {
            sendEmail(toEmail, subject, body);
        });
    }
    
    /**
     * Send email in background with audit trail (non-blocking)
     */
    public static void sendEmailAsync(String toEmail, String subject, String body, int actorId, String actorRole, String actionDescription) {
        emailExecutor.submit(() -> {
            sendEmail(toEmail, subject, body, actorId, actorRole, actionDescription);
        });
    }
    
    /**
     * Mask email for privacy in logs (shows first 3 chars + domain)
     */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "invalid@email.com";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 3) {
            return "***@" + domain;
        }
        return localPart.substring(0, 3) + "***@" + domain;
    }
    
    // ==========================================================
    // ASYNC VERSIONS (Recommended - non-blocking)
    // ==========================================================
    
    /**
     * Send welcome email after patient registration (Async) - System generated
     */
    public static void sendWelcomeEmailAsync(String patientName, String email, String username, String password) {
        String subject = "Welcome to Vantage Dental Clinic! 🦷";
        String body = "Dear " + patientName + ",\n\n" +
                      "Thank you for registering with Vantage Dental Clinic!\n\n" +
                      "Your account has been created successfully.\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "YOUR ACCOUNT INFORMATION\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "Username: " + username + "\n\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "You can now:\n" +
                      "✓ Book appointments online\n" +
                      "✓ View your medical history\n" +
                      "✓ Receive appointment reminders\n\n" +
                      "Best regards,\n" +
                      "Vantage Dental Clinic Team";

        sendEmailAsync(email, subject, body);
        addToAuditTrail(0, "System", "Email Sent", "Welcome email to new patient: " + patientName);
    }
    
    /**
     * Send appointment confirmation email (Async)
     */
    public static void sendAppointmentConfirmationAsync(String patientName, String email, 
                                                         String serviceType, String date, String time, int appointmentId) {
        sendAppointmentConfirmationWithActor(0, "System", patientName, email, serviceType, date, time, appointmentId);
    }
    
    /**
     * Send appointment confirmation email with audit trail (Async)
     */
    public static void sendAppointmentConfirmationWithActor(int actorId, String actorRole,
                                                             String patientName, String email, 
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
        
        String actionDescription = "Appointment confirmation sent to patient: " + patientName + " for appointment #" + appointmentId;
        sendEmailAsync(email, subject, body, actorId, actorRole, actionDescription);
    }
    
    /**
     * Send appointment cancellation notification (Async)
     */
    public static void sendCancellationNotificationAsync(String patientName, String email, 
                                                          String serviceType, String date, String time) {
        sendCancellationNotificationWithActor(0, "System", patientName, email, serviceType, date, time);
    }
    
    /**
     * Send appointment cancellation notification with audit trail (Async)
     */
    public static void sendCancellationNotificationWithActor(int actorId, String actorRole,
                                                              String patientName, String email, 
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
        
        String actionDescription = "Cancellation notification sent to patient: " + patientName;
        sendEmailAsync(email, subject, body, actorId, actorRole, actionDescription);
    }
    
    /**
     * Send appointment declined notification (Async)
     */
    public static void sendDeclinedNotificationAsync(String patientName, String email, 
                                                      String serviceType, String date, String time, String reason) {
        sendDeclinedNotificationWithActor(0, "System", patientName, email, serviceType, date, time, reason);
    }
    
    /**
     * Send appointment declined notification with audit trail (Async)
     */
    public static void sendDeclinedNotificationWithActor(int actorId, String actorRole,
                                                          String patientName, String email, 
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

        String actionDescription = "Declined notification sent to patient: " + patientName;
        sendEmailAsync(email, subject, body, actorId, actorRole, actionDescription);
    }
    
    /**
     * Send appointment rescheduled notification (Async)
     */
    public static void sendRescheduledNotificationAsync(String patientName, String email, 
                                                         String serviceType, String oldDate, String oldTime, 
                                                         String newDate, String newTime) {
        sendRescheduledNotificationWithActor(0, "System", patientName, email, serviceType, oldDate, oldTime, newDate, newTime);
    }
    
    /**
     * Send appointment rescheduled notification with audit trail (Async)
     */
    public static void sendRescheduledNotificationWithActor(int actorId, String actorRole,
                                                             String patientName, String email, 
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

        String actionDescription = "Reschedule notification sent to patient: " + patientName;
        sendEmailAsync(email, subject, body, actorId, actorRole, actionDescription);
    }
    
    /**
     * Send appointment reminder (day before) - Async
     */
    public static void sendAppointmentReminderAsync(String patientName, String email, 
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
        
        sendEmailAsync(email, subject, body);
    }
    
    // ==========================================================
    // SYNC VERSIONS (Blocking - for backward compatibility)
    // ==========================================================
    
    /**
     * Send welcome email after patient registration (Sync)
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
                      "Vantage Dental Clinic Team";
        
        sendEmail(email, subject, body);
    }
    
    /**
     * Send appointment confirmation email (Sync)
     */
    public static void sendAppointmentConfirmation(String patientName, String email, 
                                                     String serviceType, String date, String time, int appointmentId) {
        sendAppointmentConfirmationWithActor(0, "System", patientName, email, serviceType, date, time, appointmentId);
    }
    
    /**
     * Send appointment cancellation notification (Sync)
     */
    public static void sendCancellationNotification(String patientName, String email, 
                                                     String serviceType, String date, String time) {
        sendCancellationNotificationWithActor(0, "System", patientName, email, serviceType, date, time);
    }
    
    /**
     * Send appointment declined notification (Sync)
     */
    public static void sendDeclinedNotification(String patientName, String email, 
                                                  String serviceType, String date, String time, String reason) {
        sendDeclinedNotificationWithActor(0, "System", patientName, email, serviceType, date, time, reason);
    }
    
    /**
     * Send appointment rescheduled notification (Sync)
     */
    public static void sendRescheduledNotification(String patientName, String email, 
                                                    String serviceType, String oldDate, String oldTime, 
                                                    String newDate, String newTime) {
        sendRescheduledNotificationWithActor(0, "System", patientName, email, serviceType, oldDate, oldTime, newDate, newTime);
    }
    
    /**
     * Send appointment reminder (day before) - Sync
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
    * Send appointment reminder with audit trail (Async)
    * Automatically adjusts wording based on how far away the appointment is
    */
   public static void sendAppointmentReminderWithActor(int actorId, String actorRole,
                                                        String patientName, String email, 
                                                        String serviceType, String date, String time) {

       // Parse the appointment date
       java.time.LocalDate appointmentDate = java.time.LocalDate.parse(date);
       java.time.LocalDate today = java.time.LocalDate.now();
       long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, appointmentDate);

       String subject;
       String reminderText;

       if (daysUntil == 0) {
           subject = "Appointment Reminder - Today at Vantage Dental Clinic";
           reminderText = "This is a reminder about your appointment TODAY.";
       } else if (daysUntil == 1) {
           subject = "Appointment Reminder - Tomorrow at Vantage Dental Clinic";
           reminderText = "This is a friendly reminder about your appointment TOMORROW.";
       } else {
           subject = "Appointment Reminder - Vantage Dental Clinic";
           reminderText = "This is a reminder about your upcoming appointment on " + date + ".";
       }

       String body = "Dear " + patientName + ",\n\n" +
                     reminderText + "\n\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "APPOINTMENT DETAILS\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "Service: " + serviceType + "\n" +
                     "Date: " + date + "\n" +
                     "Time: " + time + "\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                     "Please arrive 10 minutes before your scheduled time.\n\n" +
                     "To cancel or reschedule, please contact the clinic.\n\n" +
                     "Thank you for choosing Vantage Dental Clinic!\n\n" +
                     "Best regards,\n" +
                     "Vantage Dental Clinic Team";

       String actionDescription = "Appointment reminder sent to patient: " + patientName + " (" + daysUntil + " days away)";
       sendEmailAsync(email, subject, body, actorId, actorRole, actionDescription);
   }
    
    /**
     * Check if email is configured
     */
    public static boolean isConfigured() {
        return configLoaded;
    }
    
    /**
     * Test email configuration (sends test email)
     */
    public static void testConfig() {
        System.out.println("Email configured: " + configLoaded);
        System.out.println("From Email: " + FROM_EMAIL);
        System.out.println("SMTP Host: " + SMTP_HOST);
        System.out.println("SMTP Port: " + SMTP_PORT);
        
        if (configLoaded && FROM_EMAIL != null) {
            sendEmailAsync(FROM_EMAIL, "Test Email from Dental Clinic", 
                "This is a test email to verify SMTP configuration.\n\n" +
                "If you receive this, email is working correctly.");
        }
    }
    
    /**
     * Shutdown email executor (call when app closes)
     */
    public static void shutdown() {
        emailExecutor.shutdown();
        logEmailEvent("INFO", "Email executor shutdown");
    }
    
    /**
    * Send password reset code email
    */
   public static void sendPasswordResetCode(String email, String code) {
       String subject = "Password Reset Code - Vantage Dental Clinic";
       String body = "Dear User,\n\n" +
                     "We received a request to reset your password for your Vantage Dental Clinic account.\n\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "YOUR RESET CODE\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     code + "\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                     "This code will expire in 15 minutes.\n\n" +
                     "If you did not request this, please ignore this email.\n\n" +
                     "Best regards,\n" +
                     "Vantage Dental Clinic Team";

       sendEmailAsync(email, subject, body);
   }
   
   /**
    * Send day-of appointment reminder (TODAY)
    */
   public static void sendDayOfReminderWithActor(int actorId, String actorRole,
                                                  String patientName, String email, 
                                                  String serviceType, String date, String time) {
       String subject = "🔔 Your Dental Appointment is TODAY!";
       String body = "Dear " + patientName + ",\n\n" +
                     "This is a reminder that your appointment is TODAY.\n\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "APPOINTMENT DETAILS\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                     "Service: " + serviceType + "\n" +
                     "Date: TODAY, " + date + "\n" +
                     "Time: " + time + "\n" +
                     "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                     "Please arrive 10 minutes before your scheduled time.\n\n" +
                     "To cancel or reschedule, please contact the clinic immediately.\n\n" +
                     "Best regards,\n" +
                     "Vantage Dental Clinic Team";

       String actionDescription = "Day-of reminder sent to patient: " + patientName;
       sendEmailAsync(email, subject, body, actorId, actorRole, actionDescription);
   }
}