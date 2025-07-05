package com.feri.watchmyparent.mobile.application.dto;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;

public class SensorConfigurationDTO {

    private String userId;
    private SensorType sensorType;
    private int frequencySeconds;
    private boolean isEnabled;
    private String displayName;
    private String unit;
    private int minFrequency = 30; // 30 seconds minimum
    private int maxFrequency = 1800; // 30 minutes maximum

    public SensorConfigurationDTO() {}

    public SensorConfigurationDTO(String userId, SensorType sensorType, int frequencySeconds) {
        this.userId = userId;
        this.sensorType = sensorType;
        this.frequencySeconds = frequencySeconds;
        this.isEnabled = true;
        this.displayName = sensorType.getDisplayName();
        this.unit = sensorType.getUnit();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
        if (sensorType != null) {
            this.displayName = sensorType.getDisplayName();
            this.unit = sensorType.getUnit();
        }
    }

    public int getFrequencySeconds() { return frequencySeconds; }
    public void setFrequencySeconds(int frequencySeconds) {
        if (frequencySeconds < minFrequency) {
            this.frequencySeconds = minFrequency;
        } else if (frequencySeconds > maxFrequency) {
            this.frequencySeconds = maxFrequency;
        } else {
            this.frequencySeconds = frequencySeconds;
        }
    }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getMinFrequency() { return minFrequency; }
    public int getMaxFrequency() { return maxFrequency; }

    public String getFormattedFrequency() {
        if (frequencySeconds < 60) {
            return frequencySeconds + " seconds";
        } else if (frequencySeconds < 3600) {
            return (frequencySeconds / 60) + " minutes";
        } else {
            return (frequencySeconds / 3600) + " hours";
        }
    }
}
