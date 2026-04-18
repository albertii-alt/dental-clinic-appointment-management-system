package com.dentalclinic.controller;

import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.service.StaffService;
import java.sql.SQLException;
import java.util.List;

public class AdminController {
    private final StaffService staffService = new StaffService();
    private final PatientDAO patientDAO = new PatientDAO();

    public List<Object[]> getAllStaff() throws SQLException {
        return staffService.getAllStaff();
    }

    public boolean isPatientUsernameTaken(String username) throws SQLException {
        return patientDAO.isUsernameTakenInPatients(username);
    }

    public boolean addStaff(String name, String user, String pass, String email, String role, int adminId, String adminRole) throws SQLException {
        return staffService.addStaff(name, user, pass, email, role, adminId, adminRole);
    }

    public boolean updateStaff(int targetId, String newName, String newUser, String newEmail, String newRole,
                               String newPass, int adminId, String adminRole) throws SQLException {
        return staffService.updateStaff(targetId, newName, newUser, newEmail, newRole, newPass, adminId, adminRole);
    }

    public boolean deleteStaff(int targetId, String targetName, int adminId, String adminRole) throws SQLException {
        return staffService.deleteStaff(targetId, targetName, adminId, adminRole);
    }

    public boolean toggleStaffStatus(int targetId, boolean isCurrentlyActive, int adminId, String adminRole) throws SQLException {
        return staffService.toggleStaffStatus(targetId, isCurrentlyActive, adminId, adminRole);
    }

    public boolean verifyStaffPassword(int adminId, String password) throws SQLException {
        return staffService.verifyPassword(adminId, password);
    }

    public boolean updateOwnProfile(int adminId, String newName, String newUser, String newEmail, String newPass, String adminRole) throws SQLException {
        return staffService.updateSelf(adminId, newName, newUser, newEmail, newPass, adminRole);
    }
}
