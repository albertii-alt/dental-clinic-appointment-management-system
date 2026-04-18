package com.dentalclinic.model;

import java.sql.Timestamp;

public class ClinicHour {
    private int slotId;
    private String timeSlot;
    private boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ClinicHour(int slotId, String timeSlot, boolean active, Timestamp createdAt, Timestamp updatedAt) {
        this.slotId = slotId;
        this.timeSlot = timeSlot;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getSlotId() { return slotId; }
    public String getTimeSlot() { return timeSlot; }
    public boolean isActive() { return active; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
}
