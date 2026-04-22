package com.dentalclinic.dto.appointment;

import java.sql.Date;

public class AppointmentRequest {
    private final int patientId;
    private final String serviceType;
    private final Date appointmentDate;
    private final String appointmentTime;
    private final int ageAtVisit;
    private final String contactNumber;
    private int actorId;
    private String actorRole;

    public AppointmentRequest(int patientId, String serviceType, Date appointmentDate, String appointmentTime, int ageAtVisit, String contactNumber) {
        this.patientId = patientId;
        this.serviceType = serviceType;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.ageAtVisit = ageAtVisit;
        this.contactNumber = contactNumber;
    }

    public AppointmentRequest(int patientId, String serviceType, java.util.Date appointmentDate, String appointmentTime, int ageAtVisit, String contactNumber) {
        this(
                patientId,
                serviceType,
                new Date(appointmentDate.getTime()),
                appointmentTime,
                ageAtVisit,
                contactNumber
        );
    }

    public AppointmentRequest withActor(int actorId, String actorRole) {
        this.actorId = actorId;
        this.actorRole = actorRole;
        return this;
    }

    public int getPatientId() { return patientId; }
    public String getServiceType() { return serviceType; }
    public Date getAppointmentDate() { return appointmentDate; }
    public String getAppointmentTime() { return appointmentTime; }
    public int getAgeAtVisit() { return ageAtVisit; }
    public String getContactNumber() { return contactNumber; }
    public int getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
}
