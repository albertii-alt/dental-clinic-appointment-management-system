package com.dentalclinic.model;

public enum AppointmentStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    DECLINED("Declined"),
    RESCHEDULED("Rescheduled"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String dbValue;

    AppointmentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AppointmentStatus fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        for (AppointmentStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown appointment status: " + value);
    }
}
