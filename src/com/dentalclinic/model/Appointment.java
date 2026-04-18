package com.dentalclinic.model;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class Appointment {
    private int appointmentId; 
    private int patientId;
    private Integer serviceId;
    private String serviceType;
    private Date appointmentDate;
    private Time appointmentTimeNew;
    private String appointmentTime;
    private int ageAtVisit;
    private String contactAtVisit;
    private String status;
    private String clinical_notes;
    private Timestamp requestDate;
    private boolean isRead; // New field for notification status
    private boolean isArchived;
    private boolean reminderSent;
    private boolean dayOfReminderSent;
    private DentalService service;
    private Patient patient;

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    // Constructor for creating a NEW appointment (ID not known yet)
      // Constructor 1: For NEW appointments (No ID, No Notes, No isRead yet)
    public Appointment(int pId, String service, Date date, String time, int age, String contact, String status) {
        this.patientId = pId;
        this.serviceType = service;
        this.appointmentDate = date;
        this.appointmentTime = time;
        this.serviceId = null;
        this.appointmentTimeNew = null;
        this.ageAtVisit = age;
        this.contactAtVisit = contact;
        this.status = status;
        this.requestDate = null;
        this.isRead = false; // Default for new ones
        this.isArchived = false;
        this.reminderSent = false;
        this.dayOfReminderSent = false;
    }

    // Constructor 2: The COMPLETE one for loading from Database (10 arguments)
    public Appointment(int appId, int pId, String service, Date date, String time, int age, String contact, String status, String clinical_notes, boolean isRead) {
        this.appointmentId = appId;
        this.patientId = pId;
        this.serviceId = null;
        this.serviceType = service;
        this.appointmentDate = date;
        this.appointmentTimeNew = null;
        this.appointmentTime = time;
        this.ageAtVisit = age;
        this.contactAtVisit = contact;
        this.status = status;
        this.clinical_notes = clinical_notes;
        this.requestDate = null;
        this.isRead = isRead;
        this.isArchived = false;
        this.reminderSent = false;
        this.dayOfReminderSent = false;
    }

    // Constructor 3: Full domain constructor mapped from appointments table
    public Appointment(
            int appointmentId,
            int patientId,
            Integer serviceId,
            String serviceType,
            Date appointmentDate,
            Time appointmentTimeNew,
            int ageAtVisit,
            String contactAtVisit,
            String status,
            Timestamp requestDate,
            String clinicalNotes,
            boolean isRead,
            boolean isArchived,
            boolean reminderSent,
            boolean dayOfReminderSent
    ) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.serviceId = serviceId;
        this.serviceType = serviceType;
        this.appointmentDate = appointmentDate;
        this.appointmentTimeNew = appointmentTimeNew;
        this.appointmentTime = appointmentTimeNew != null ? appointmentTimeNew.toString() : null;
        this.ageAtVisit = ageAtVisit;
        this.contactAtVisit = contactAtVisit;
        this.status = status;
        this.requestDate = requestDate;
        this.clinical_notes = clinicalNotes;
        this.isRead = isRead;
        this.isArchived = isArchived;
        this.reminderSent = reminderSent;
        this.dayOfReminderSent = dayOfReminderSent;
    }

    // Getters
    public int getAppointmentId() { return appointmentId; } // CRITICAL FOR CANCELLING
    public int getPatientId() { return patientId; }
    public Integer getServiceId() { return serviceId; }
    public String getServiceType() { return serviceType; }
    public Date getAppointmentDate() { return appointmentDate; }
    public Time getAppointmentTimeNew() { return appointmentTimeNew; }
    public String getAppointmentTime() { return appointmentTime; }
    public int getAgeAtVisit() { return ageAtVisit; }
    public String getContactAtVisit() { return contactAtVisit; }
    public String getStatus() { return status; }
    public String getClinicalNotes() { return clinical_notes; }
    public Timestamp getRequestDate() { return requestDate; }
    public boolean isReminderSent() { return reminderSent; }
    public boolean isDayOfReminderSent() { return dayOfReminderSent; }
    public DentalService getService() { return service; }
    public Patient getPatient() { return patient; }

    public void setClinicalNotes(String notes) { this.clinical_notes = notes; }
    public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }
    public void setAppointmentTimeNew(Time appointmentTimeNew) {
        this.appointmentTimeNew = appointmentTimeNew;
        this.appointmentTime = appointmentTimeNew != null ? appointmentTimeNew.toString() : this.appointmentTime;
    }
    public void setRequestDate(Timestamp requestDate) { this.requestDate = requestDate; }
    public void setReminderSent(boolean reminderSent) { this.reminderSent = reminderSent; }
    public void setDayOfReminderSent(boolean dayOfReminderSent) { this.dayOfReminderSent = dayOfReminderSent; }
    public void setService(DentalService service) { this.service = service; }
    public void setPatient(Patient patient) { this.patient = patient; }

    // Add these to Appointment.java
    public void setStatus(String status) { 
        this.status = status; 
    }

    public void setServiceType(String serviceType) { 
        this.serviceType = serviceType; 
    }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
