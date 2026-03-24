package com.dentalclinic.model;

import java.sql.Date;

public class Patient {
    private int patientId;
    private String firstName;
    private String middleName;
    private String lastName;
    private Date birthDate;
    private int age;
    private String address, contactNumber, email, username;

    // Constructor to quickly build a Patient object
    public Patient(int id, String fName, String mName, String lName, Date dob, int age, String addr, String phone, String email, String user) {
        this.patientId = id;
        this.firstName = fName;
        this.middleName = mName;
        this.lastName = lName;
        this.birthDate = dob;
        this.age = age;
        this.address = addr;
        this.contactNumber = phone;
        this.email = email;
        this.username = user;
    }

    // Getters - These allow the UI and DAO to "ask" for data safely
    public int getPatientId() { return patientId; }
    public String getFullName() { return firstName + " " + lastName; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public Date getBirthDate() { return birthDate; }
    public int getAge() { return age; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }
    public String getUsername() { return username; }
    public String getEmail() {return email;}
}