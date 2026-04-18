package com.dentalclinic.controller;

import com.dentalclinic.dto.appointment.AppointmentRequest;
import com.dentalclinic.dto.appointment.BookingResult;
import com.dentalclinic.model.Appointment;
import com.dentalclinic.model.Patient;
import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.service.PatientService;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class AppointmentController {
    private final AppointmentService appointmentService = new AppointmentService();
    private final PatientService patientService = new PatientService();

    public String[] getServiceList() throws SQLException {
        return appointmentService.getServiceList();
    }

    public List<String> getClosedDays() throws SQLException {
        return appointmentService.getClosedDays();
    }

    public int getBookingLeadTime() throws SQLException {
        return appointmentService.getBookingLeadTime();
    }

    public List<String> getAvailableSlotsForDate(Date date) throws SQLException {
        return appointmentService.getAvailableSlotsForDate(date);
    }

    public BookingResult bookForPatient(AppointmentRequest request) throws SQLException {
        return appointmentService.bookAppointment(request, false);
    }

    public BookingResult bookAndApproveByStaff(AppointmentRequest request) throws SQLException {
        return appointmentService.bookAppointment(request, true);
    }

    public List<Object[]> searchPatientsByName(String query) throws SQLException {
        return patientService.searchPatientsByName(query);
    }

    public Patient getPatientById(int patientId) throws SQLException {
        return patientService.getPatientById(patientId);
    }

    public boolean updateAppointmentStatus(int appointmentId, String status, int actorId, String actorRole) throws SQLException {
        return appointmentService.updateAppointmentStatus(appointmentId, status, actorId, actorRole);
    }

    public List<Appointment> getPatientAppointmentHistory(int patientId) throws SQLException {
        return appointmentService.getPatientAppointmentHistory(patientId);
    }

    public List<Appointment> getAppointmentsByPatient(int patientId) throws SQLException {
        return appointmentService.getAppointmentsByPatient(patientId);
    }

    public List<Appointment> getTodaysAppointmentsByPatient(int patientId) throws Exception {
        return appointmentService.getTodaysAppointmentsByPatient(patientId);
    }

    public List<Appointment> getUpcomingScheduleByPatient(int patientId) throws SQLException {
        return appointmentService.getUpcomingScheduleByPatient(patientId);
    }

    public List<Object[]> getPendingRequestsWithNames() throws SQLException {
        return appointmentService.getPendingRequestsWithNames();
    }

    public List<Appointment> getUpcomingAppointments() throws Exception {
        return appointmentService.getUpcomingAppointments();
    }

    public List<Object[]> getTodaysSchedule() throws SQLException {
        return appointmentService.getTodaysSchedule();
    }

    public List<Appointment> getTodaysAppointments() throws SQLException {
        return appointmentService.getTodaysAppointments();
    }

    public boolean deleteAppointment(int appointmentId) {
        return appointmentService.deleteAppointment(appointmentId);
    }

    public boolean rescheduleAppointment(int appId, java.sql.Date date, String time, int actorId, String actorRole) throws SQLException {
        return appointmentService.rescheduleAppointment(appId, date, time, actorId, actorRole);
    }

    public boolean markNotificationAsRead(int appointmentId) {
        return appointmentService.markNotificationAsRead(appointmentId);
    }

    public boolean archiveNotification(int appointmentId) throws SQLException {
        return appointmentService.archiveNotification(appointmentId);
    }

    public boolean archiveAllNotifications(int patientId) throws SQLException {
        return appointmentService.archiveAllNotifications(patientId);
    }

    public List<String> getOccupiedSlots(java.sql.Date date) throws SQLException {
        return appointmentService.getOccupiedSlots(date);
    }

    public List<String> getBlockedSlotsByDate(java.sql.Date date) throws SQLException {
        return appointmentService.getBlockedSlotsByDate(date);
    }

    public boolean blockSlot(java.sql.Date date, String slot, String reason, int staffId, String role) throws SQLException {
        return appointmentService.blockSlot(date, slot, reason, staffId, role);
    }

    public boolean unblockSlot(java.sql.Date date, String slot, int staffId, String role) throws SQLException {
        return appointmentService.unblockSlot(date, slot, staffId, role);
    }

    public void blockAllDay(java.sql.Date date, String[] slots, int staffId, String role) throws SQLException {
        appointmentService.blockAllDay(date, slots, staffId, role);
    }

    public boolean unblockAllDay(java.sql.Date date, int staffId, String role) throws SQLException {
        return appointmentService.unblockAllDay(date, staffId, role);
    }

    public String[] getTimeSlots() throws SQLException {
        return appointmentService.getTimeSlots();
    }
}
