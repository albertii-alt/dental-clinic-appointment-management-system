package com.dentalclinic.service;

import com.dentalclinic.dao.PatientDAO;
import com.dentalclinic.model.Patient;
import java.sql.SQLException;
import java.util.List;

public class PatientService {
    private final PatientDAO patientDAO = new PatientDAO();

    public List<Object[]> searchPatientsByName(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return patientDAO.getAllPatients();
        }
        return patientDAO.searchPatientsByName(query.trim());
    }

    public Patient getPatientById(int patientId) throws SQLException {
        return patientDAO.getPatientById(patientId);
    }

    public boolean verifyPassword(int patientId, String currentPassword) throws SQLException {
        return patientDAO.verifyPassword(patientId, currentPassword);
    }

    public boolean updateFullProfile(int id, String fName, String mName, String lName, java.sql.Date dob, int age,
                                     String addr, String phone, String email, String user, String newPass) throws SQLException {
        return patientDAO.updateFullProfile(id, fName, mName, lName, dob, age, addr, phone, email, user, newPass);
    }
}
