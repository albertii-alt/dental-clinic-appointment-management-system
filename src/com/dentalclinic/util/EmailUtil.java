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
     * Build complete HTML email wrapper with consistent styling (Gmail-safe inline CSS)
     */
    private static String buildHtmlEmail(String accentColor, String icon, String title, String bodyHtml) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head><meta charset=\"UTF-8\"></head>" +
               "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;\">" +
               "<div style=\"background-color:#f4f4f4;padding:30px;text-align:center;\">" +
               "<div style=\"max-width:600px;margin:0 auto;background-color:#ffffff;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1);overflow:hidden;\">" +
               "<div style=\"height:8px;background-color:" + accentColor + ";\"></div>" +
               "<div style=\"padding:20px 0 10px 0;text-align:center;background-color:#ffffff;\">" +
               "<span style=\"font-size:28px;font-weight:bold;color:#2c3e50;\">🦷 Vantage Dental Clinic</span>" +
               "</div>" +
               "<div style=\"text-align:center;padding:16px 0 8px 0;\">" +
               "<div style=\"font-size:48px;\">" + icon + "</div>" +
               "<div style=\"font-size:24px;font-weight:bold;color:#333333;margin-top:12px;\">" + title + "</div>" +
               "</div>" +
               "<div style=\"padding:24px;color:#333333;line-height:1.5;\">" + bodyHtml + "</div>" +
               "<div style=\"background-color:#f0f0f0;padding:16px;text-align:center;font-size:12px;color:#888888;\">" +
               "© Vantage Dental Clinic · This is an automated message, please do not reply" +
               "</div>" +
               "</div>" +
               "</div>" +
               "</body>" +
               "</html>";
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
            message.setContent(body, "text/html; charset=utf-8");
            
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
            message.setContent(body, "text/html; charset=utf-8");
            
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
     *
     * SECURITY FIX: Password is no longer accepted or included in the email body.
     * The patient just typed their password during registration — they already know it.
     * Sending passwords via email exposes them to interception, mail server logging,
     * and forwarding risks. Only the username is included for reference.
     */
    public static void sendWelcomeEmailAsync(String patientName, String email, String username) {
        String subject = "Welcome to Vantage Dental Clinic!";
        
        String accountInfoHtml = "<div style=\"background:#f8f9fa;border-left:4px solid #2E86AB;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                  "<p style=\"margin:4px 0;\"><strong>Username:</strong> " + escapeHtml(username) + "</p>" +
                                  "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>Thank you for registering with Vantage Dental Clinic!</p>" +
                          "<p>Your account has been created successfully.</p>" +
                          accountInfoHtml +
                          "<p style=\"color:#d9534f; font-size:14px;\"><strong>Security Note:</strong> For your protection, passwords are never included in emails. If you ever forget your password, use the 'Forgot Password' option on the login screen to reset it.</p>" +
                          "<p>You can now:</p>" +
                          "<ul>" +
                          "<li>Book appointments online</li>" +
                          "<li>View your medical history</li>" +
                          "<li>Receive appointment reminders</li>" +
                          "</ul>" +
                          "<p>To login, open the Dental Clinic application.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#2E86AB", "🎉", "Welcome to Vantage Dental Clinic!", bodyHtml);
        sendEmailAsync(email, subject, htmlEmail);
        addToAuditTrail(0, "System", "Email Sent", "Welcome email sent to new patient: " + patientName);
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
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid #27AE60;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Reference ID:</strong> #" + appointmentId + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>Your appointment has been <strong style=\"color:#27AE60;\">CONFIRMED</strong>!</p>" +
                          appointmentDetails +
                          "<p>Please arrive 10 minutes before your scheduled time.</p>" +
                          "<p>To cancel or reschedule, please contact the clinic.</p>" +
                          "<p>Thank you for choosing Vantage Dental Clinic!</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#27AE60", "✅", "Appointment Confirmed", bodyHtml);
        String actionDescription = "Appointment confirmation sent to patient: " + patientName + " for appointment #" + appointmentId;
        sendEmailAsync(email, subject, htmlEmail, actorId, actorRole, actionDescription);
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
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid #E74C3C;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>We regret to inform you that your appointment has been <strong style=\"color:#E74C3C;\">CANCELLED</strong>.</p>" +
                          appointmentDetails +
                          "<p>Please contact the clinic to reschedule.</p>" +
                          "<p>We apologize for any inconvenience.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#E74C3C", "❌", "Appointment Cancelled", bodyHtml);
        String actionDescription = "Cancellation notification sent to patient: " + patientName;
        sendEmailAsync(email, subject, htmlEmail, actorId, actorRole, actionDescription);
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
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid #E67E22;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     (reason != null && !reason.isEmpty() ? "<p style=\"margin:4px 0;\"><strong>Reason:</strong> " + escapeHtml(reason) + "</p>" : "") +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>We regret to inform you that your appointment request has been <strong style=\"color:#E67E22;\">DECLINED</strong>.</p>" +
                          appointmentDetails +
                          "<p>Please contact the clinic to schedule a different time.</p>" +
                          "<p>We apologize for any inconvenience.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#E67E22", "⚠️", "Appointment Declined", bodyHtml);
        String actionDescription = "Declined notification sent to patient: " + patientName;
        sendEmailAsync(email, subject, htmlEmail, actorId, actorRole, actionDescription);
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
        
        String oldAppointment = "<div style=\"background:#f8f9fa;border-left:4px solid #aaaaaa;border-radius:4px;padding:16px;margin:8px 0;\">" +
                                 "<p style=\"margin:2px 0;font-size:11px;color:#888888;text-transform:uppercase;letter-spacing:1px;\">Previous Schedule</p>" +
                                 "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                 "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(oldDate) + "</p>" +
                                 "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(oldTime) + "</p>" +
                                 "</div>";
        
        String newAppointment = "<div style=\"background:#f8f9fa;border-left:4px solid #8E44AD;border-radius:4px;padding:16px;margin:8px 0;\">" +
                                 "<p style=\"margin:2px 0;font-size:11px;color:#8E44AD;text-transform:uppercase;letter-spacing:1px;\">New Schedule</p>" +
                                 "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                 "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(newDate) + "</p>" +
                                 "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(newTime) + "</p>" +
                                 "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>Your appointment has been <strong style=\"color:#8E44AD;\">RESCHEDULED</strong>.</p>" +
                          oldAppointment +
                          newAppointment +
                          "<p>If this new time doesn't work for you, please contact the clinic.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#8E44AD", "📅", "Appointment Rescheduled", bodyHtml);
        String actionDescription = "Reschedule notification sent to patient: " + patientName;
        sendEmailAsync(email, subject, htmlEmail, actorId, actorRole, actionDescription);
    }
    
    /**
     * Send appointment reminder (day before) - Async
     */
    public static void sendAppointmentReminderAsync(String patientName, String email, 
                                                     String serviceType, String date, String time) {
        String subject = "Appointment Reminder - Tomorrow at Vantage Dental Clinic";
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid #F39C12;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>This is a reminder about your appointment <strong>TOMORROW</strong>.</p>" +
                          appointmentDetails +
                          "<p>Please bring any relevant medical records.</p>" +
                          "<p>We look forward to seeing you!</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#F39C12", "🔔", "Appointment Reminder", bodyHtml);
        sendEmailAsync(email, subject, htmlEmail);
    }
    
    // ==========================================================
    // SYNC VERSIONS (Blocking - for backward compatibility)
    // ==========================================================
    
    /**
     * Send welcome email after patient registration (Sync)
     *
     * SECURITY FIX: Password parameter removed. Passwords must never be sent
     * via email. Only the username is included so the patient has their login
     * reference without exposing their credentials.
     */
    public static void sendWelcomeEmail(String patientName, String email, String username) {
        String subject = "Welcome to Vantage Dental Clinic!";
        
        String accountInfoHtml = "<div style=\"background:#f8f9fa;border-left:4px solid #2E86AB;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                  "<p style=\"margin:4px 0;\"><strong>Username:</strong> " + escapeHtml(username) + "</p>" +
                                  "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>Thank you for registering with Vantage Dental Clinic!</p>" +
                          "<p>Your account has been created successfully.</p>" +
                          accountInfoHtml +
                          "<p style=\"color:#d9534f; font-size:14px;\"><strong>Security Note:</strong> For your protection, passwords are never included in emails. If you ever forget your password, use the 'Forgot Password' option on the login screen to reset it.</p>" +
                          "<p>You can now:</p>" +
                          "<ul>" +
                          "<li>Book appointments online</li>" +
                          "<li>View your medical history</li>" +
                          "<li>Receive appointment reminders</li>" +
                          "</ul>" +
                          "<p>To login, open the Dental Clinic application.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#2E86AB", "🎉", "Welcome to Vantage Dental Clinic!", bodyHtml);
        sendEmail(email, subject, htmlEmail);
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
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid #F39C12;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>This is a reminder about your appointment <strong>TOMORROW</strong>.</p>" +
                          appointmentDetails +
                          "<p>Please bring any relevant medical records.</p>" +
                          "<p>We look forward to seeing you!</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#F39C12", "🔔", "Appointment Reminder", bodyHtml);
        sendEmail(email, subject, htmlEmail);
    }
    
    /**
     * Send appointment reminder with audit trail (Async)
     * Automatically adjusts wording based on how far away the appointment is
     */
    public static void sendAppointmentReminderWithActor(int actorId, String actorRole,
                                                         String patientName, String email, 
                                                         String serviceType, String date, String time) {

        java.time.LocalDate appointmentDate = java.time.LocalDate.parse(date);
        java.time.LocalDate today = java.time.LocalDate.now();
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, appointmentDate);

        String subject;
        String reminderText;
        String accentColor;
        String icon;

        if (daysUntil == 0) {
            subject = "Appointment Reminder - Today at Vantage Dental Clinic";
            reminderText = "This is a reminder about your appointment <strong>TODAY</strong>.";
            accentColor = "#E74C3C";
            icon = "⏰";
        } else if (daysUntil == 1) {
            subject = "Appointment Reminder - Tomorrow at Vantage Dental Clinic";
            reminderText = "This is a friendly reminder about your appointment <strong>TOMORROW</strong>.";
            accentColor = "#F39C12";
            icon = "🔔";
        } else {
            subject = "Appointment Reminder - Vantage Dental Clinic";
            reminderText = "This is a reminder about your upcoming appointment on " + escapeHtml(date) + ".";
            accentColor = "#F39C12";
            icon = "🔔";
        }
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid " + accentColor + ";border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>" + reminderText + "</p>" +
                          appointmentDetails +
                          "<p>Please arrive 10 minutes before your scheduled time.</p>" +
                          "<p>To cancel or reschedule, please contact the clinic.</p>" +
                          "<p>Thank you for choosing Vantage Dental Clinic!</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String title = daysUntil == 0 ? "Your Dental Appointment is TODAY!" : "Appointment Reminder";
        String htmlEmail = buildHtmlEmail(accentColor, icon, title, bodyHtml);
        String actionDescription = "Appointment reminder sent to patient: " + patientName + " (" + daysUntil + " days away)";
        sendEmailAsync(email, subject, htmlEmail, actorId, actorRole, actionDescription);
    }
    
    /**
     * Send password reset code email
     */
    public static void sendPasswordResetCode(String email, String code) {
        String subject = "Password Reset Code - Vantage Dental Clinic";
        
        String resetCodeBox = "<div style=\"background:#f0f0f0;border-radius:8px;padding:20px;text-align:center;margin:20px 0;letter-spacing:8px;font-size:32px;font-weight:bold;color:#2C3E50;\">" +
                               escapeHtml(code) +
                               "</div>";
        
        String bodyHtml = "<p>Dear User,</p>" +
                          "<p>We received a request to reset your password for your Vantage Dental Clinic account.</p>" +
                          "<p><strong>Your password reset code is:</strong></p>" +
                          resetCodeBox +
                          "<p style=\"color:#d9534f;\">This code will expire in 15 minutes.</p>" +
                          "<p>If you did not request this, please ignore this email.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#2C3E50", "🔐", "Password Reset Request", bodyHtml);
        sendEmailAsync(email, subject, htmlEmail);
    }
    
    /**
     * Send day-of appointment reminder (TODAY)
     */
    public static void sendDayOfReminderWithActor(int actorId, String actorRole,
                                                   String patientName, String email, 
                                                   String serviceType, String date, String time) {
        String subject = "Your Dental Appointment is TODAY!";
        
        String appointmentDetails = "<div style=\"background:#f8f9fa;border-left:4px solid #E74C3C;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                     "<p style=\"margin:4px 0;\"><strong>Service:</strong> " + escapeHtml(serviceType) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Date:</strong> TODAY, " + escapeHtml(date) + "</p>" +
                                     "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>" +
                                     "</div>";
        
        String bodyHtml = "<p>Dear " + escapeHtml(patientName) + ",</p>" +
                          "<p>This is a reminder that your appointment is <strong style=\"color:#E74C3C;\">TODAY</strong>.</p>" +
                          appointmentDetails +
                          "<p>Please arrive 10 minutes before your scheduled time.</p>" +
                          "<p>To cancel or reschedule, please contact the clinic immediately.</p>" +
                          "<p>Best regards,<br>Vantage Dental Clinic Team</p>";
        
        String htmlEmail = buildHtmlEmail("#E74C3C", "⏰", "Your Dental Appointment is TODAY!", bodyHtml);
        String actionDescription = "Day-of reminder sent to patient: " + patientName;
        sendEmailAsync(email, subject, htmlEmail, actorId, actorRole, actionDescription);
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
            String testBodyHtml = "<p>This is a test email to verify SMTP configuration.</p>" +
                                   "<div style=\"background:#f8f9fa;border-left:4px solid #2E86AB;border-radius:4px;padding:16px;margin:16px 0;\">" +
                                   "<p style=\"margin:4px 0;\"><strong>Status:</strong> ✅ Email system is working correctly</p>" +
                                   "<p style=\"margin:4px 0;\"><strong>Time:</strong> " + new java.util.Date() + "</p>" +
                                   "</div>" +
                                   "<p>If you receive this, HTML email rendering is working properly in your email client.</p>";
            
            String htmlEmail = buildHtmlEmail("#2E86AB", "📧", "Test Email", testBodyHtml);
            sendEmailAsync(FROM_EMAIL, "Test Email from Dental Clinic", htmlEmail);
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
     * Escape HTML special characters to prevent injection
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}