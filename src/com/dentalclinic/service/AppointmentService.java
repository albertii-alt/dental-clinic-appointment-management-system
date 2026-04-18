package com.dentalclinic.service;

import com.dentalclinic.dao.AppointmentDAO;
import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.dto.appointment.AppointmentRequest;
import com.dentalclinic.dto.appointment.BookingResult;
import com.dentalclinic.model.Appointment;
import java.sql.SQLException;
import java.util.List;
import com.dentalclinic.util.EmailUtil;
import com.dentalclinic.util.Sanitizer;

public class AppointmentService {
    private AppointmentDAO appointmentDAO = new AppointmentDAO();
    private PatientDAO patientDAO = new PatientDAO();
    private LogService logService = new LogService();

    public String[] getServiceList() throws SQLException {
        return appointmentDAO.getDynamicServices();
    }

    public String[] getTimeSlots() throws SQLException {
        return appointmentDAO.getDynamicTimeSlots();
    }

    public List<String> getClosedDays() throws SQLException {
        return appointmentDAO.getClosedDaysFromDB();
    }

    public int getBookingLeadTime() throws SQLException {
        return appointmentDAO.getLeadTime();
    }

    public int createAppointment(Appointment app) throws SQLException {
        return appointmentDAO.save(app); 
    }

    public BookingResult bookAppointment(AppointmentRequest request, boolean autoApprove) throws SQLException {
        if (request.getPatientId() <= 0) {
            return new BookingResult(false, "Please select a valid patient.", -1);
        }
        if (request.getAppointmentDate() == null) {
            return new BookingResult(false, "Please select an appointment date.", -1);
        }
        if (request.getServiceType() == null || request.getServiceType().trim().isEmpty()) {
            return new BookingResult(false, "Please select a service.", -1);
        }
        if (request.getAppointmentTime() == null || request.getAppointmentTime().trim().isEmpty()) {
            return new BookingResult(false, "Please select a time slot.", -1);
        }
        if (!Sanitizer.isValidPhone(request.getContactNumber())) {
            return new BookingResult(false, "Please enter a valid contact number.", -1);
        }

        if (!autoApprove && appointmentDAO.hasPendingAppointment(request.getPatientId())) {
            return new BookingResult(false, "You currently have a request pending approval.", -1);
        }

        java.time.LocalDate selectedDate = request.getAppointmentDate().toLocalDate();
        java.time.LocalDate today = java.time.LocalDate.now();
        int leadDays = appointmentDAO.getLeadTime();
        if (selectedDate.isBefore(today.plusDays(leadDays))) {
            return new BookingResult(false, "Selected date violates booking lead time.", -1);
        }

        List<String> closedDays = appointmentDAO.getClosedDaysFromDB();
        String selectedDayName = selectedDate.getDayOfWeek().name().substring(0, 1) +
                selectedDate.getDayOfWeek().name().substring(1).toLowerCase();
        if (closedDays.stream().anyMatch(day -> day.equalsIgnoreCase(selectedDayName))) {
            return new BookingResult(false, "Clinic is closed on the selected date.", -1);
        }

        List<String> activeSlots = java.util.Arrays.asList(appointmentDAO.getDynamicTimeSlots());
        if (!activeSlots.contains(request.getAppointmentTime())) {
            return new BookingResult(false, "Selected time slot is not available.", -1);
        }

        List<String> occupiedSlots = appointmentDAO.getOccupiedSlots(request.getAppointmentDate());
        List<String> blockedSlots = appointmentDAO.getBlockedSlotsByDate(request.getAppointmentDate());
        if (occupiedSlots.contains(request.getAppointmentTime()) || blockedSlots.contains(request.getAppointmentTime())) {
            return new BookingResult(false, "Selected time slot is no longer available.", -1);
        }

        String status = autoApprove ? "Approved" : "Pending";
        Appointment appointment = new Appointment(
                request.getPatientId(),
                request.getServiceType(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                request.getAgeAtVisit(),
                request.getContactNumber(),
                status
        );

        int generatedId = appointmentDAO.save(appointment);
        if (generatedId == -1) {
            return new BookingResult(false, "Failed to save appointment.", -1);
        }
        return new BookingResult(true, autoApprove ? "Appointment booked and approved." : "Appointment request submitted.", generatedId);
    }
    
    public List<Appointment> getPatientAppointmentHistory(int patientId) throws SQLException {
        return appointmentDAO.getAppointmentsByPatient(patientId);
    }

    public List<Appointment> getAppointmentsByPatient(int patientId) throws SQLException {
        return appointmentDAO.getAppointmentsByPatient(patientId);
    }
    
    public boolean updateAppointmentStatus(int appId, String status) throws SQLException {
        return appointmentDAO.updateStatus(appId, status);
    }
    
    public boolean updateAppointmentStatus(int appId, String status, int actorId, String actorRole) throws SQLException {
        String serviceName = appointmentDAO.getServiceNameByAppId(appId);
        Appointment appointment = appointmentDAO.getAppointmentById(appId);
            com.dentalclinic.model.Patient patient = null;
        if (appointment != null) {
            patient = patientDAO.getPatientById(appointment.getPatientId());
        }

        boolean success = appointmentDAO.updateStatus(appId, status);

        if (success) {
            String detailedLog = "Service: " + serviceName + " | Appt #" + appId + " set to " + status;
            logService.record(actorId, actorRole, "Status Update", detailedLog);

            if (status.equalsIgnoreCase("Approved") && patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                EmailUtil.sendAppointmentConfirmationWithActor(
                    actorId, actorRole,
                    patient.getFirstName() + " " + patient.getLastName(),
                    patient.getEmail(),
                    serviceName,
                    appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime(),
                    appId
                );
            }

            if (status.equalsIgnoreCase("Cancelled") && patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                EmailUtil.sendCancellationNotificationWithActor(
                    actorId, actorRole,
                    patient.getFirstName() + " " + patient.getLastName(),
                    patient.getEmail(),
                    serviceName,
                    appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime()
                );
            }

            if (status.equalsIgnoreCase("Declined") && patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                String reason = appointment.getClinicalNotes();
                EmailUtil.sendDeclinedNotificationWithActor(
                    actorId, actorRole,
                    patient.getFirstName() + " " + patient.getLastName(),
                    patient.getEmail(),
                    serviceName,
                    appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime(),
                    reason
                );
            }
        }
        return success;
    }

    public List<String> getAllTimeSlots() throws SQLException {
        return appointmentDAO.getAllTimeSlotsFromDB();
    }
    
    public List<Object[]> getFullServiceList() throws SQLException {
        return appointmentDAO.getServiceDetails();
    }
    
    public List<String> getAvailableSlotsForDate(java.util.Date date) throws SQLException {
        String[] allSlotsArray = appointmentDAO.getDynamicTimeSlots(); 
        List<String> availableSlots = new java.util.ArrayList<>(java.util.Arrays.asList(allSlotsArray));
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        List<String> occupiedSlots = appointmentDAO.getOccupiedSlots(sqlDate);
        List<String> staffBlockedSlots = appointmentDAO.getBlockedSlotsByDate(sqlDate);
        availableSlots.removeAll(occupiedSlots);
        availableSlots.removeAll(staffBlockedSlots);
        return availableSlots;
    }

    public boolean canPatientBook(int pId) throws SQLException {
        return !appointmentDAO.hasPendingAppointment(pId);
    }
    
    public List<Appointment> getPendingRequests() throws SQLException {
        return appointmentDAO.getAppointmentsByStatus("Pending");
    }

    public List<Appointment> getDeclinedRequests() throws SQLException {
        return appointmentDAO.getAppointmentsByStatus("Declined");
    }

    public List<Appointment> getTodaysAppointments() throws SQLException {
        return appointmentDAO.getAppointmentsByDateRange(true);
    }

    public List<Appointment> getUpcomingAppointments() throws Exception {
        return appointmentDAO.getApprovedUpcomingAppointments(); 
    }
    
    public List<Appointment> getAllAppointments() throws SQLException {
        return appointmentDAO.getAllAppointments();
    }
    
    public List<Object[]> getPendingRequestsWithNames() throws SQLException {
        return appointmentDAO.getPendingAppointmentsWithNames();
    }
    
    public boolean rescheduleAppointment(int appId, java.sql.Date newDate, String newTime) throws SQLException {
        return appointmentDAO.updateDateTime(appId, newDate, newTime);
    }
    
    public boolean rescheduleAppointment(int appId, java.sql.Date newDate, String newTime, int actorId, String actorRole) throws SQLException {
        String serviceName = appointmentDAO.getServiceNameByAppId(appId);
        Appointment originalApp = appointmentDAO.getAppointmentById(appId);
        String oldDate = originalApp.getAppointmentDate().toString();
        String oldTime = originalApp.getAppointmentTime();
        com.dentalclinic.model.Patient patient = null;
        if (originalApp != null) {
            patient = patientDAO.getPatientById(originalApp.getPatientId());
        }
        boolean success = appointmentDAO.updateDateTime(appId, newDate, newTime); 
        if (success) {
            String details = "Service: " + serviceName + " | Rescheduled Appt #" + appId + " to " + newDate + " at " + newTime;
            logService.record(actorId, actorRole, "Reschedule", details);
            if (patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                EmailUtil.sendRescheduledNotificationWithActor(
                    actorId, actorRole,
                    patient.getFirstName() + " " + patient.getLastName(),
                    patient.getEmail(),
                    serviceName,
                    oldDate, oldTime,
                    newDate.toString(), newTime
                );
            }
        }
        return success;
    }
    
    public List<Object[]> getCancelledRequestsWithNames() throws SQLException {
        return appointmentDAO.getCancelledAppointmentsWithNames();
    }
    
    public List<Object[]> getTodaysSchedule() throws SQLException {
        return appointmentDAO.getTodaysAppointmentsWithNames();
    }
    
    public List<Object[]> getTreatmentHistory() throws SQLException {
        return appointmentDAO.getCompletedAppointmentsWithNames();
    }
    
    public boolean updateTreatmentRecord(int appId, String status, String notes) throws SQLException {
        return appointmentDAO.updateTreatmentRecord(appId, status, notes);
    }

    public boolean updateTreatmentRecord(int appId, String status, String notes, int actorId, String actorRole) throws SQLException {
        String serviceName = appointmentDAO.getServiceNameByAppId(appId);
        boolean success = appointmentDAO.updateTreatmentRecord(appId, status, notes);
        if (success) {
            String details = "Service: " + serviceName + " | Appt #" + appId + " completed. Notes: " + notes;
            logService.record(actorId, actorRole, "Treatment Recorded", details);
        }
        return success;
    }
    
    public List<Appointment> getTodaysAppointmentsByPatient(int pId) throws Exception {
        return appointmentDAO.getTodaysAppointmentsByPatient(pId);
    }
    
    public List<Appointment> getPatientHistory(int pId) throws Exception {
        return appointmentDAO.getAppointmentsByPatient(pId);
    }
    
    public List<Appointment> getCancelledAppointmentsByPatient(int pId) throws Exception {
        return appointmentDAO.getAppointmentsByPatient(pId).stream()
                .filter(a -> a.getStatus().equalsIgnoreCase("Cancelled"))
                .collect(java.util.stream.Collectors.toList());
    }
    
    public List<Appointment> getUpcomingScheduleByPatient(int pId) throws SQLException {
        return appointmentDAO.getUpcomingScheduleByPatient(pId);
    }

    public List<Appointment> getPatientNotifications(int pId) throws SQLException {
        return appointmentDAO.getAppointmentsByPatient(pId).stream()
                .filter(a -> !a.getStatus().equalsIgnoreCase("Pending"))
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean markNotificationAsRead(int appId) {
        return appointmentDAO.markAsRead(appId);
    }

    public boolean archiveNotification(int appId) throws SQLException {
        return appointmentDAO.archiveNotification(appId);
    }

    public boolean archiveAllNotifications(int patientId) throws SQLException {
        return appointmentDAO.archiveAllNotifications(patientId);
    }
    
    public boolean clearAllCancelledAppointments() {
        try {
            return appointmentDAO.clearCancelledOrDeclinedAppointments();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteAppointment(int appointmentId) {
        try {
            return appointmentDAO.deleteById(appointmentId);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Appointment> getAutoArchivedCancelled(int pId) throws SQLException {
        return appointmentDAO.getRecentCancelledByPatient(pId, 30);
    }

    public List<String> getOccupiedSlots(java.sql.Date date) throws SQLException {
        return appointmentDAO.getOccupiedSlots(date);
    }

    public List<String> getBlockedSlotsByDate(java.sql.Date date) throws SQLException {
        return appointmentDAO.getBlockedSlotsByDate(date);
    }

    public boolean blockSlot(java.sql.Date date, String slot, String reason, int staffId, String role) throws SQLException {
        return appointmentDAO.blockSlot(date, slot, reason, staffId, role);
    }

    public boolean unblockSlot(java.sql.Date date, String slot, int staffId, String role) throws SQLException {
        return appointmentDAO.unblockSlot(date, slot, staffId, role);
    }

    public void blockAllDay(java.sql.Date date, String[] slots, int staffId, String role) throws SQLException {
        appointmentDAO.blockAllDay(date, slots, staffId, role);
    }

    public boolean unblockAllDay(java.sql.Date date, int staffId, String role) throws SQLException {
        return appointmentDAO.unblockAllDay(date, staffId, role);
    }

    // ==========================================================
    // EMAIL REMINDER METHODS (NEW)
    // ==========================================================

    public List<Appointment> getAppointmentsForTomorrow() throws SQLException {
        return appointmentDAO.getAppointmentsForTomorrow();
    }

    public boolean sendReminderForAppointment(int appId, int actorId, String actorRole) throws SQLException {
        Appointment appointment = appointmentDAO.getAppointmentByIdForReminder(appId);
        if (appointment == null) {
            return false;
        }
        com.dentalclinic.model.Patient patient = patientDAO.getPatientById(appointment.getPatientId());
        if (patient == null || patient.getEmail() == null || patient.getEmail().isEmpty()) {
            return false;
        }
        EmailUtil.sendAppointmentReminderWithActor(
            actorId, actorRole,
            patient.getFirstName() + " " + patient.getLastName(),
            patient.getEmail(),
            appointment.getServiceType(),
            appointment.getAppointmentDate().toString(),
            appointment.getAppointmentTime()
        );
        return appointmentDAO.markReminderSent(appId);
    }

    public int sendAllRemindersForTomorrow() throws SQLException {
        List<Appointment> appointments = getAppointmentsForTomorrow();
        int sentCount = 0;
        for (Appointment app : appointments) {
            try {
                if (sendReminderForAppointment(app.getAppointmentId(), 0, "System")) {
                    sentCount++;
                    System.out.println("Reminder sent for appointment #" + app.getAppointmentId());
                }
            } catch (Exception e) {
                System.err.println("Failed to send reminder for appointment #" + app.getAppointmentId() + ": " + e.getMessage());
            }
        }
        return sentCount;
    }
    
    // ==========================================================
    // DAY-OF REMINDER METHODS
    // ==========================================================

    /**
     * Get appointments for TODAY
     */
    public List<Appointment> getAppointmentsForToday() throws SQLException {
        return appointmentDAO.getAppointmentsForToday();
    }

    /**
     * Send day-of reminder for a single appointment
     */
    public boolean sendDayOfReminderForAppointment(int appId, int actorId, String actorRole) throws SQLException {
        Appointment appointment = appointmentDAO.getAppointmentByIdForReminder(appId);
        if (appointment == null) {
            return false;
        }
        com.dentalclinic.model.Patient patient = patientDAO.getPatientById(appointment.getPatientId());
        if (patient == null || patient.getEmail() == null || patient.getEmail().isEmpty()) {
            return false;
        }

        EmailUtil.sendDayOfReminderWithActor(
            actorId, actorRole,
            patient.getFirstName() + " " + patient.getLastName(),
            patient.getEmail(),
            appointment.getServiceType(),
            appointment.getAppointmentDate().toString(),
            appointment.getAppointmentTime()
        );
        return appointmentDAO.markDayOfReminderSent(appId);
    }

    /**
     * Send all day-of reminders for TODAY (called on app startup)
     */
    public int sendAllDayOfReminders() throws SQLException {
        List<Appointment> appointments = getAppointmentsForToday();
        int sentCount = 0;
        for (Appointment app : appointments) {
            try {
                if (sendDayOfReminderForAppointment(app.getAppointmentId(), 0, "System")) {
                    sentCount++;
                    System.out.println("Day-of reminder sent for appointment #" + app.getAppointmentId());
                }
            } catch (Exception e) {
                System.err.println("Failed to send day-of reminder for appointment #" + app.getAppointmentId() + ": " + e.getMessage());
            }
        }
        return sentCount;
    }
}
