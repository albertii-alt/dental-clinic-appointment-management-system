package com.dentalclinic.model;

import java.sql.Date;
import java.sql.Timestamp;

public class Patient {
    private int patientId;
    private String firstName;
    private String middleName;
    private String lastName;
    private Date birthDate;
    private int age;
    private String address, contactNumber, email, username;
    private Timestamp registrationDate;
    private boolean forcePasswordReset;
    private int failedLoginAttempts;
    private boolean accountLocked;
    private Timestamp lockoutTime;

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
        this.registrationDate = null;
        this.forcePasswordReset = false;
        this.failedLoginAttempts = 0;
        this.accountLocked = false;
        this.lockoutTime = null;
    }

    public Patient(
            int patientId,
            String firstName,
            String middleName,
            String lastName,
            Date birthDate,
            int age,
            String address,
            String contactNumber,
            String email,
            String username,
            Timestamp registrationDate,
            boolean forcePasswordReset,
            int failedLoginAttempts,
            boolean accountLocked,
            Timestamp lockoutTime
    ) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.age = age;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
        this.username = username;
        this.registrationDate = registrationDate;
        this.forcePasswordReset = forcePasswordReset;
        this.failedLoginAttempts = failedLoginAttempts;
        this.accountLocked = accountLocked;
        this.lockoutTime = lockoutTime;
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
    public Timestamp getRegistrationDate() { return registrationDate; }
    public boolean isForcePasswordReset() { return forcePasswordReset; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public boolean isAccountLocked() { return accountLocked; }
    public Timestamp getLockoutTime() { return lockoutTime; }

    // Setters for mutable domain updates
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }
    public void setAge(int age) { this.age = age; }
    public void setAddress(String address) { this.address = address; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setRegistrationDate(Timestamp registrationDate) { this.registrationDate = registrationDate; }
    public void setForcePasswordReset(boolean forcePasswordReset) { this.forcePasswordReset = forcePasswordReset; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }
    public void setLockoutTime(Timestamp lockoutTime) { this.lockoutTime = lockoutTime; }
}
