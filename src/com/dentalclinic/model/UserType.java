package com.dentalclinic.model;

public enum UserType {
    PATIENT("patient"),
    STAFF("staff");

    private final String dbValue;

    UserType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static UserType fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        for (UserType type : values()) {
            if (type.dbValue.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown user type: " + value);
    }
}
