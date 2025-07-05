package com.feri.watchmyparent.mobile.application.dto;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;
import java.time.LocalDateTime;

public class SensorDataDTO {

    private String userId;
    private SensorType sensorType;
    private double value;
    private String unit;
    private LocalDateTime timestamp;
    private String deviceId;
    private boolean isTransmitted;

    public SensorDataDTO() {}

    public SensorDataDTO(String userId, SensorType sensorType, double value, String deviceId) {
        this.userId = userId;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = sensorType.getUnit();
        this.deviceId = deviceId;
        this.timestamp = LocalDateTime.now();
        this.isTransmitted = false;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public boolean isTransmitted() { return isTransmitted; }
    public void setTransmitted(boolean transmitted) { isTransmitted = transmitted; }

    public String getFormattedValue() {
        return String.format("%.2f %s", value, unit);
    }
}
