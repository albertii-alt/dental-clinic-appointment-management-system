package com.dentalclinic.model;

import java.sql.Timestamp;

public class ClinicScheduleDay {
    private int dayId;
    private String dayName;
    private boolean open;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ClinicScheduleDay(int dayId, String dayName, boolean open, Timestamp createdAt, Timestamp updatedAt) {
        this.dayId = dayId;
        this.dayName = dayName;
        this.open = open;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getDayId() { return dayId; }
    public String getDayName() { return dayName; }
    public boolean isOpen() { return open; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
}
