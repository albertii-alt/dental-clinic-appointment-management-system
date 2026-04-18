package com.dentalclinic.controller;

import com.dentalclinic.model.Patient;
import com.dentalclinic.service.PatientService;
import java.sql.SQLException;
import java.util.Date;

public class PatientController {
    private final PatientService patientService = new PatientService();

    public Patient getPatientById(int patientId) throws SQLException {
        return patientService.getPatientById(patientId);
    }

    public boolean verifyPassword(int patientId, String currentPassword) throws SQLException {
        return patientService.verifyPassword(patientId, currentPassword);
    }

    public boolean updateFullProfile(int id, String fName, String mName, String lName, java.sql.Date dob, int age,
                                     String addr, String phone, String email, String user, String newPass) throws SQLException {
        return patientService.updateFullProfile(id, fName, mName, lName, dob, age, addr, phone, email, user, newPass);
    }

    public boolean updateFullProfile(int id, String fName, String mName, String lName, Date dob, int age,
                                     String addr, String phone, String email, String user, String newPass) throws SQLException {
        java.sql.Date sqlDob = new java.sql.Date(dob.getTime());
        return patientService.updateFullProfile(id, fName, mName, lName, sqlDob, age, addr, phone, email, user, newPass);
    }
}
