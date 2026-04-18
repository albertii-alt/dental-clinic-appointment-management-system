package com.dentalclinic.model;

import java.sql.Date;

public class BlockedSlot {
    private int blockId;
    private Date blockDate;
    private String timeSlot;
    private String reason;

    public BlockedSlot(int blockId, Date blockDate, String timeSlot, String reason) {
        this.blockId = blockId;
        this.blockDate = blockDate;
        this.timeSlot = timeSlot;
        this.reason = reason;
    }

    public int getBlockId() { return blockId; }
    public Date getBlockDate() { return blockDate; }
    public String getTimeSlot() { return timeSlot; }
    public String getReason() { return reason; }
}
