package com.dentalclinic.model;

public class ClinicSetting {
    private int settingId;
    private String settingName;
    private int settingValue;

    public ClinicSetting(int settingId, String settingName, int settingValue) {
        this.settingId = settingId;
        this.settingName = settingName;
        this.settingValue = settingValue;
    }

    public int getSettingId() { return settingId; }
    public String getSettingName() { return settingName; }
    public int getSettingValue() { return settingValue; }
}
