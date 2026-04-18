package com.dentalclinic.service;

import com.dentalclinic.dao.StaffDAO;
import java.sql.SQLException;
import java.util.List;

public class StaffService {
    private final StaffDAO staffDAO = new StaffDAO();

    public List<Object[]> getAllStaff() throws SQLException {
        return staffDAO.getAllStaff();
    }

    public boolean addStaff(String name, String user, String pass, String email, String role, int adminId, String adminRole) throws SQLException {
        return staffDAO.addStaff(name, user, pass, email, role, adminId, adminRole);
    }

    public boolean updateStaff(int targetId, String newName, String newUser, String newEmail, String newRole,
                               String newPass, int adminId, String adminRole) throws SQLException {
        return staffDAO.updateStaff(targetId, newName, newUser, newEmail, newRole, newPass, adminId, adminRole);
    }

    public boolean deleteStaff(int targetId, String targetName, int adminId, String adminRole) throws SQLException {
        return staffDAO.deleteStaff(targetId, targetName, adminId, adminRole);
    }

    public boolean toggleStaffStatus(int targetId, boolean isCurrentlyActive, int adminId, String adminRole) throws SQLException {
        return staffDAO.toggleStaffStatus(targetId, isCurrentlyActive, adminId, adminRole);
    }

    public boolean verifyPassword(int adminId, String password) throws SQLException {
        return staffDAO.verifyPassword(adminId, password);
    }

    public boolean updateSelf(int adminId, String newName, String newUser, String newEmail, String newPass, String adminRole) throws SQLException {
        return staffDAO.updateSelf(adminId, newName, newUser, newEmail, newPass, adminRole);
    }
}
