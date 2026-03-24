package com.dentalclinic.model;

import java.sql.Date;

public class Appointment {
    private int appointmentId; 
    private int patientId;
    private String serviceType;
    private Date appointmentDate;
    private String appointmentTime;
    private int ageAtVisit;
    private String contactAtVisit;
    private String status;
    private String clinical_notes;
    private boolean isRead; // New field for notification status
    private boolean isArchived;
    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    // Constructor for creating a NEW appointment (ID not known yet)
      // Constructor 1: For NEW appointments (No ID, No Notes, No isRead yet)
    public Appointment(int pId, String service, Date date, String time, int age, String contact, String status) {
        this.patientId = pId;
        this.serviceType = service;
        this.appointmentDate = date;
        this.appointmentTime = time;
        this.ageAtVisit = age;
        this.contactAtVisit = contact;
        this.status = status;
        this.isRead = false; // Default for new ones
        this.isArchived = false;
    }

    // Constructor 2: The COMPLETE one for loading from Database (10 arguments)
    public Appointment(int appId, int pId, String service, Date date, String time, int age, String contact, String status, String clinical_notes, boolean isRead) {
        this.appointmentId = appId;
        this.patientId = pId;
        this.serviceType = service;
        this.appointmentDate = date;
        this.appointmentTime = time;
        this.ageAtVisit = age;
        this.contactAtVisit = contact;
        this.status = status;
        this.clinical_notes = clinical_notes;
        this.isRead = isRead;
        this.isArchived = false;
    }

    // Getters
    public int getAppointmentId() { return appointmentId; } // CRITICAL FOR CANCELLING
    public int getPatientId() { return patientId; }
    public String getServiceType() { return serviceType; }
    public Date getAppointmentDate() { return appointmentDate; }
    public String getAppointmentTime() { return appointmentTime; }
    public int getAgeAtVisit() { return ageAtVisit; }
    public String getContactAtVisit() { return contactAtVisit; }
    public String getStatus() { return status; }
    public String getClinicalNotes() { return clinical_notes; }
    public void setClinicalNotes(String notes) { this.clinical_notes = notes; }
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