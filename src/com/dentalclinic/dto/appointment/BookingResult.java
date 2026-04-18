package com.dentalclinic.dto.appointment;

public class BookingResult {
    private final boolean success;
    private final String message;
    private final int appointmentId;

    public BookingResult(boolean success, String message, int appointmentId) {
        this.success = success;
        this.message = message;
        this.appointmentId = appointmentId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getAppointmentId() {
        return appointmentId;
    }
}
