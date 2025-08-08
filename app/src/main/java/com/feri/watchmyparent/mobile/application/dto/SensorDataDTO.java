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

    // ✅ NEW fields for transmission tracking
    private LocalDateTime transmissionTime;
    private String transmissionMethod;
    private int retryCount;
    private String errorMessage;

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
    public void setTransmitted(boolean transmitted) {
        isTransmitted = transmitted;
        if (transmitted && transmissionTime == null) {
            transmissionTime = LocalDateTime.now();
        }
    }

    // ✅ NEW getters and setters for transmission tracking
    public LocalDateTime getTransmissionTime() {
        return transmissionTime;
    }

    public void setTransmissionTime(LocalDateTime transmissionTime) {
        this.transmissionTime = transmissionTime;
    }

    public String getTransmissionMethod() {
        return transmissionMethod;
    }

    public void setTransmissionMethod(String transmissionMethod) {
        this.transmissionMethod = transmissionMethod;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // ✅ Utility methods
    public String getFormattedValue() {
        if (unit != null && !unit.isEmpty()) {
            return String.format("%.2f %s", value, unit);
        } else {
            return String.format("%.2f", value);
        }
    }

    public boolean isRecentlyTransmitted(int maxAgeMinutes) {
        if (!isTransmitted || transmissionTime == null) return false;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        return transmissionTime.isAfter(cutoff);
    }

    public boolean needsRetransmission() {
        return !isTransmitted && retryCount < 3; // Max 3 retries
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void markAsTransmitted(String method) {
        this.isTransmitted = true;
        this.transmissionTime = LocalDateTime.now();
        this.transmissionMethod = method;
        this.errorMessage = null;
    }

    public void markAsFailedTransmission(String error) {
        this.isTransmitted = false;
        this.errorMessage = error;
        this.incrementRetryCount();
    }

    public String getTransmissionStatus() {
        if (isTransmitted) {
            return "✅ Transmitted via " + (transmissionMethod != null ? transmissionMethod : "Unknown");
        } else if (retryCount > 0) {
            return "🔄 Failed (" + retryCount + " retries)";
        } else {
            return "⏳ Pending";
        }
    }

    @Override
    public String toString() {
        return String.format("SensorDataDTO{userId='%s', type=%s, value=%.2f %s, transmitted=%s, device='%s'}",
                userId, sensorType, value, unit, isTransmitted, deviceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SensorDataDTO that = (SensorDataDTO) obj;
        return Double.compare(that.value, value) == 0 &&
                isTransmitted == that.isTransmitted &&
                (userId != null ? userId.equals(that.userId) : that.userId == null) &&
                sensorType == that.sensorType &&
                (timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null);
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (sensorType != null ? sensorType.hashCode() : 0);
        long temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (isTransmitted ? 1 : 0);
        return result;
    }
}