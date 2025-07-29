package com.feri.watchmyparent.mobile.domain.valueobjects;

import com.feri.watchmyparent.mobile.domain.enums.SensorType;

import java.time.LocalDateTime;
import java.util.Objects;

public class SensorReading {

    private SensorType sensorType;
    private double value;
    private LocalDateTime timestamp;
    private String unit;

    // ✅ NEW: Additional fields for real device integration
    private String deviceId;
    private String connectionType;
    private double accuracy;
    private String metadata;

    //Constructors
    public SensorReading() {}

    public SensorReading(SensorType sensorType, double value) {
        this.sensorType = sensorType;
        this.value = value;
        this.timestamp = LocalDateTime.now();
        this.unit = sensorType != null ? sensorType.getUnit() : "";
    }

    public SensorReading(SensorType sensorType, double value, String deviceId) {
        this(sensorType, value);
        this.deviceId = deviceId;
    }

    // Getters and Setters
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
        // Auto-set unit when sensor type changes
        if (sensorType != null && (this.unit == null || this.unit.isEmpty())) {
            this.unit = sensorType.getUnit();
        }
    }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    // ✅ NEW getters and setters for device integration
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }


    // ✅ Utility methods
    public boolean isValid() {
        return sensorType != null && !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public boolean isRecent(int maxAgeMinutes) {
        if (timestamp == null) return false;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        return timestamp.isAfter(cutoff);
    }

    public String getFormattedValue() {
        if (unit != null && !unit.isEmpty()) {
            return String.format("%.2f %s", value, unit);
        } else {
            return String.format("%.2f", value);
        }
    }

    @Override
    public String toString() {
        return String.format("SensorReading{type=%s, value=%.2f %s, device=%s, timestamp=%s}",
                sensorType, value, unit, deviceId, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SensorReading that = (SensorReading) obj;
        return Double.compare(that.value, value) == 0 &&
                sensorType == that.sensorType &&
                (timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null) &&
                (deviceId != null ? deviceId.equals(that.deviceId) : that.deviceId == null);
    }

    @Override
    public int hashCode() {
        int result = sensorType != null ? sensorType.hashCode() : 0;
        long temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        return result;
    }
}
