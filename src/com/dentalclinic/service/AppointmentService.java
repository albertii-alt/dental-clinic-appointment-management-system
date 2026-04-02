package com.dentalclinic.service;

import com.dentalclinic.dao.AppointmentDAO;
import com.dentalclinic.model.Appointment;
import java.sql.SQLException;
import java.util.List;
import com.dentalclinic.util.DBConnection; // Added for Database access
import java.sql.Connection;               // Added to fix your error
import java.sql.PreparedStatement;
import com.dentalclinic.util.EmailUtil;

public class AppointmentService {
    private AppointmentDAO appointmentDAO = new AppointmentDAO();
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
    public List<Appointment> getPatientAppointmentHistory(int patientId) throws SQLException {
        return appointmentDAO.getAppointmentsByPatient(patientId);
        }
    
    public boolean updateAppointmentStatus(int appId, String status) throws SQLException {
        return appointmentDAO.updateStatus(appId, status);
        }
    public boolean updateAppointmentStatus(int appId, String status, int actorId, String actorRole) throws SQLException {
        // 1. Fetch the service type from the DAO first
        String serviceName = appointmentDAO.getServiceNameByAppId(appId);

        // 2. Get appointment and patient details for email (before updating)
        Appointment appointment = appointmentDAO.getAppointmentById(appId);
        com.dentalclinic.model.Patient patient = null;
        if (appointment != null) {
            com.dentalclinic.dao.PatientDAO patientDAO = new com.dentalclinic.dao.PatientDAO();
            patient = patientDAO.getPatientById(appointment.getPatientId());
        }

        // 3. Perform the update
        boolean success = appointmentDAO.updateStatus(appId, status);

        if (success) {
            // 4. Log the action
            String detailedLog = "Service: " + serviceName + " | Appt #" + appId + " set to " + status;
            logService.record(actorId, actorRole, "Status Update", detailedLog);

            // 5. Send email notification for approval
            if (status.equalsIgnoreCase("Approved") && patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                EmailUtil.sendAppointmentConfirmation(
                    patient.getFirstName() + " " + patient.getLastName(),
                    patient.getEmail(),
                    serviceName,
                    appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime(),
                    appId
                );
            }

            // 6. Send email notification for cancellation
            if (status.equalsIgnoreCase("Cancelled") && patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                EmailUtil.sendCancellationNotification(
                    patient.getFirstName() + " " + patient.getLastName(),
                    patient.getEmail(),
                    serviceName,
                    appointment.getAppointmentDate().toString(),
                    appointment.getAppointmentTime()
                );
            }

            // 7. Send email notification for declined (NEW)
            if (status.equalsIgnoreCase("Declined") && patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                String reason = appointment.getClinicalNotes();
                EmailUtil.sendDeclinedNotification(
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

        // Add this to com.dentalclinic.service.AppointmentService
    public List<String> getAllTimeSlots() throws SQLException {
        return appointmentDAO.getAllTimeSlotsFromDB();
    }
    
    public List<Object[]> getFullServiceList() throws SQLException {
        return appointmentDAO.getServiceDetails();
    }
    
    public List<String> getAvailableSlotsForDate(java.util.Date date) throws SQLException {
    // 1. Get ALL active slots the Admin defined in clinic_hours
        String[] allSlotsArray = appointmentDAO.getDynamicTimeSlots(); 
        List<String> availableSlots = new java.util.ArrayList<>(java.util.Arrays.asList(allSlotsArray));

        java.sql.Date sqlDate = new java.sql.Date(date.getTime());

        // 2. Get the slots already booked by patients
        List<String> occupiedSlots = appointmentDAO.getOccupiedSlots(sqlDate);

        // 3. Get the slots manually blocked by staff
        List<String> staffBlockedSlots = appointmentDAO.getBlockedSlotsByDate(sqlDate);

        // 4. Filter them all out
        availableSlots.removeAll(occupiedSlots);
        availableSlots.removeAll(staffBlockedSlots);

        return availableSlots;
    }

    public boolean canPatientBook(int pId) throws SQLException {
        return !appointmentDAO.hasPendingAppointment(pId);
    }
    
    // --- NEW STAFF SERVICE METHODS ---

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
        // Change this to call the new 'Approved' only method
        return appointmentDAO.getApprovedUpcomingAppointments(); 
    }
    // Add this to your existing AppointmentService.java
    public List<Appointment> getAllAppointments() throws SQLException {
        return appointmentDAO.getAllAppointments();
    }
    public List<Object[]> getPendingRequestsWithNames() throws SQLException {
        return appointmentDAO.getPendingAppointmentsWithNames();
    }
    public boolean rescheduleAppointment(int appId, java.sql.Date newDate, String newTime) throws SQLException {
        return appointmentDAO.updateDateTime(appId, newDate, newTime);
    }
    // Updated Reschedule with Service Type Formatting
    public boolean rescheduleAppointment(int appId, java.sql.Date newDate, String newTime, int actorId, String actorRole) throws SQLException {
        // 1. Fetch Service Name first
        String serviceName = appointmentDAO.getServiceNameByAppId(appId);

        // 2. Get original appointment details for email
        Appointment originalApp = appointmentDAO.getAppointmentById(appId);
        String oldDate = originalApp.getAppointmentDate().toString();
        String oldTime = originalApp.getAppointmentTime();

        // 3. Get patient details for email
        com.dentalclinic.model.Patient patient = null;
        if (originalApp != null) {
            com.dentalclinic.dao.PatientDAO patientDAO = new com.dentalclinic.dao.PatientDAO();
            patient = patientDAO.getPatientById(originalApp.getPatientId());
        }

        // 4. Perform the actual reschedule
        boolean success = appointmentDAO.updateDateTime(appId, newDate, newTime); 

        // 5. Log the action
        if (success) {
            String details = "Service: " + serviceName + " | Rescheduled Appt #" + appId + " to " + newDate + " at " + newTime;
            logService.record(actorId, actorRole, "Reschedule", details);

            // 6. Send email notification
            if (patient != null && patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                EmailUtil.sendRescheduledNotification(
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
// Updated Treatment Record with Service Type Formatting
    public boolean updateTreatmentRecord(int appId, String status, String notes, int actorId, String actorRole) throws SQLException {
        // 1. Fetch Service Name first
        String serviceName = appointmentDAO.getServiceNameByAppId(appId);
        
        boolean success = appointmentDAO.updateTreatmentRecord(appId, status, notes);
        
        if (success) {
            // Include the service name so the Admin can see what treatment was performed in the log modal
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
    // 1. Fetch upcoming appointments (Today and Future)
    public List<Appointment> getUpcomingScheduleByPatient(int pId) throws SQLException {
        return appointmentDAO.getUpcomingScheduleByPatient(pId);
    }

    // 2. Fetch Notifications (Updates from Staff/Dentist)
    public List<Appointment> getPatientNotifications(int pId) throws SQLException {
        // We reuse the DAO logic to get all appointments, but you can 
        // eventually filter this by a 'is_read' flag or specific statuses
        return appointmentDAO.getAppointmentsByPatient(pId).stream()
                .filter(a -> !a.getStatus().equalsIgnoreCase("Pending"))
                .collect(java.util.stream.Collectors.toList());
    }
    
    public boolean clearAllCancelledAppointments() {
        String sql = "DELETE FROM appointments WHERE status IN ('Cancelled', 'Declined')";
        try (Connection conn = com.dentalclinic.util.DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteAppointment(int appointmentId) {
        String query = "DELETE FROM appointments WHERE appointment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, appointmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Add this to AppointmentService.java
    public List<Appointment> getAutoArchivedCancelled(int pId) throws SQLException {
        // We set the limit to 30 days. Anything older is "Archived" (hidden)
        return appointmentDAO.getRecentCancelledByPatient(pId, 30);
    }
}