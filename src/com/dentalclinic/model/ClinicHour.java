package com.dentalclinic.model;

import java.util.Date;

public class ClinicHour {
    private int slotId;
    private String timeSlot;
    private boolean active;
    private Date createdAt;
    private Date updatedAt;

    public ClinicHour(int slotId, String timeSlot, boolean active, Date createdAt, Date updatedAt) {
        this.slotId = slotId;
        this.timeSlot = timeSlot;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getSlotId() { return slotId; }
    public String getTimeSlot() { return timeSlot; }
    public boolean isActive() { return active; }
    public Date getCreatedAt() { return createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
}
