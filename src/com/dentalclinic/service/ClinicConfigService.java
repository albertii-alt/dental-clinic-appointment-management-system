package com.dentalclinic.service;

import com.dentalclinic.dao.ClinicConfigDAO;
import java.sql.SQLException;

public class ClinicConfigService {
    private final ClinicConfigDAO clinicConfigDAO = new ClinicConfigDAO();

    public boolean updateLeadTime(int newDays, int staffId, String role) throws SQLException {
        return clinicConfigDAO.updateLeadTime(newDays, staffId, role);
    }

    public boolean updateTimeSlotStatus(String timeSlot, boolean isActive, int staffId, String role) throws SQLException {
        return clinicConfigDAO.updateTimeSlotStatus(timeSlot, isActive, staffId, role);
    }

    public boolean updateDayStatus(String dayName, boolean isOpen, int staffId, String role) throws SQLException {
        return clinicConfigDAO.updateDayStatus(dayName, isOpen, staffId, role);
    }

    public boolean addTimeSlot(String newTime, int staffId, String role) throws SQLException {
        return clinicConfigDAO.addTimeSlot(newTime, staffId, role);
    }

    public boolean deleteTimeSlot(String timeSlot, int staffId, String role) throws SQLException {
        return clinicConfigDAO.deleteTimeSlot(timeSlot, staffId, role);
    }

    public boolean updateServiceStatus(String serviceName, boolean shouldBeActive, int staffId, String role) throws SQLException {
        return clinicConfigDAO.updateServiceStatus(serviceName, shouldBeActive, staffId, role);
    }

    public boolean deleteService(String serviceName, int staffId, String role) throws SQLException {
        return clinicConfigDAO.deleteService(serviceName, staffId, role);
    }

    public boolean addService(String name, String desc, double price, int staffId, String role) throws SQLException {
        return clinicConfigDAO.addService(name, desc, price, staffId, role);
    }

    public boolean updateService(String oldName, String newName, String description, double price, int staffId, String role) throws SQLException {
        return clinicConfigDAO.updateService(oldName, newName, description, price, staffId, role);
    }

    public Object[] getServiceDetailsByName(String serviceName) throws SQLException {
        return clinicConfigDAO.getServiceDetailsByName(serviceName);
    }
}
