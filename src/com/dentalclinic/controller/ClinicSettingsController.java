package com.dentalclinic.controller;

import com.dentalclinic.service.AppointmentService;
import com.dentalclinic.service.ClinicConfigService;
import java.sql.SQLException;
import java.util.List;

public class ClinicSettingsController {
    private final AppointmentService appointmentService = new AppointmentService();
    private final ClinicConfigService clinicConfigService = new ClinicConfigService();

    public int getBookingLeadTime() throws SQLException {
        return appointmentService.getBookingLeadTime();
    }

    public List<String> getClosedDays() throws SQLException {
        return appointmentService.getClosedDays();
    }

    public List<String> getAllTimeSlots() throws SQLException {
        return appointmentService.getAllTimeSlots();
    }

    public String[] getActiveTimeSlots() throws SQLException {
        return appointmentService.getTimeSlots();
    }

    public List<Object[]> getServiceList() throws SQLException {
        return appointmentService.getFullServiceList();
    }

    public boolean updateLeadTime(int newDays, int staffId, String role) throws SQLException {
        return clinicConfigService.updateLeadTime(newDays, staffId, role);
    }

    public boolean updateDayStatus(String dayName, boolean isOpen, int staffId, String role) throws SQLException {
        return clinicConfigService.updateDayStatus(dayName, isOpen, staffId, role);
    }

    public boolean updateTimeSlotStatus(String timeSlot, boolean isActive, int staffId, String role) throws SQLException {
        return clinicConfigService.updateTimeSlotStatus(timeSlot, isActive, staffId, role);
    }

    public boolean addTimeSlot(String newTime, int staffId, String role) throws SQLException {
        return clinicConfigService.addTimeSlot(newTime, staffId, role);
    }

    public boolean deleteTimeSlot(String timeSlot, int staffId, String role) throws SQLException {
        return clinicConfigService.deleteTimeSlot(timeSlot, staffId, role);
    }

    public boolean addService(String name, String desc, double price, int staffId, String role) throws SQLException {
        return clinicConfigService.addService(name, desc, price, staffId, role);
    }

    public boolean updateServiceStatus(String serviceName, boolean isActive, int staffId, String role) throws SQLException {
        return clinicConfigService.updateServiceStatus(serviceName, isActive, staffId, role);
    }

    public boolean deleteService(String serviceName, int staffId, String role) throws SQLException {
        return clinicConfigService.deleteService(serviceName, staffId, role);
    }

    public Object[] getServiceDetailsByName(String serviceName) throws SQLException {
        return clinicConfigService.getServiceDetailsByName(serviceName);
    }

    public boolean updateService(String oldName, String newName, String newDesc, double newPrice, int staffId, String role) throws SQLException {
        return clinicConfigService.updateService(oldName, newName, newDesc, newPrice, staffId, role);
    }
}
