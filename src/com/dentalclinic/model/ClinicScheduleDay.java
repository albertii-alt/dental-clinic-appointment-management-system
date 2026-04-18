package com.dentalclinic.model;

import java.util.Date;

public class ClinicScheduleDay {
    private int dayId;
    private String dayName;
    private boolean open;
    private Date createdAt;
    private Date updatedAt;

    public ClinicScheduleDay(int dayId, String dayName, boolean open, Date createdAt, Date updatedAt) {
        this.dayId = dayId;
        this.dayName = dayName;
        this.open = open;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getDayId() { return dayId; }
    public String getDayName() { return dayName; }
    public boolean isOpen() { return open; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}
